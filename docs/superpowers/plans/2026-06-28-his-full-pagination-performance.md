# HIS Full Pagination And Performance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement true end-to-end pagination across HIS list pages, remove oversized client-side fetch/filter patterns, and add targeted SQL/index optimizations without changing existing business field meaning.

**Architecture:** Backend list endpoints will move filtering, pagination, and heavy aggregation into database-backed mapper queries or batched lookups. Frontend pages will stop fetching oversized pages and instead bind Element Plus pagination controls to backend `PageResponse` results with shared query-state behavior.

**Tech Stack:** Spring Boot 3, MyBatis-Plus, MySQL schema SQL, Vue 3, Element Plus, Axios, Maven, Vite

---

### Task 1: Lock In Backend Pagination Regressions With Failing Tests

**Files:**
- Create: `neusoft-his-system/his-service/src/test/java/com/neusoft/his/service/doctor/DoctorWorkstationServicePaginationTest.java`
- Create: `neusoft-his-system/his-service/src/test/java/com/neusoft/his/service/patient/PatientServicePaginationTest.java`
- Create: `neusoft-his-system/his-common/src/test/java/com/neusoft/his/common/audit/DbAuditServicePaginationTest.java`
- Modify: `neusoft-his-system/his-service/pom.xml`

- [ ] **Step 1: Write the failing doctor schedule pagination test**

```java
@Test
void scheduleQuery_should_delegate_filter_and_paging_to_mapper() {
    DoctorScheduleMapper scheduleMapper = mock(DoctorScheduleMapper.class);
    DoctorProfileMapper doctorMapper = mock(DoctorProfileMapper.class);
    MedicalRecordMapper recordMapper = mock(MedicalRecordMapper.class);
    OutpatientRegistrationMapper registrationMapper = mock(OutpatientRegistrationMapper.class);
    PrescriptionMapper prescriptionMapper = mock(PrescriptionMapper.class);
    AuditService auditService = mock(AuditService.class);
    DoctorWorkstationService service = new DoctorWorkstationService(
            doctorMapper, scheduleMapper, recordMapper, registrationMapper, prescriptionMapper, auditService
    );

    when(scheduleMapper.selectSchedulePage(any(Page.class), eq("心血管内科"), eq("张"), eq("2026-06-28")))
            .thenReturn(List.of(new DoctorScheduleView(1L, 11L, "2026-06-28", "上午", "心血管内科", "张医生", "张医生", "主任医师", "上午", "专家号", 20, 6, 1)));

    PageResponse<DoctorScheduleView> response = service.scheduleQuery("心血管内科", "张", "2026-06-28", 2, 5);

    assertThat(response.records()).hasSize(1);
    assertThat(response.total()).isEqualTo(1);
    verify(scheduleMapper).selectSchedulePage(any(Page.class), eq("心血管内科"), eq("张"), eq("2026-06-28"));
    verify(scheduleMapper).countSchedulePage(eq("心血管内科"), eq("张"), eq("2026-06-28"));
    verifyNoInteractions(doctorMapper, registrationMapper);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl neusoft-his-system/his-service -am -Dtest=DoctorWorkstationServicePaginationTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: FAIL because `selectSchedulePage` / `countSchedulePage` do not exist yet.

- [ ] **Step 3: Write the failing registration pagination test**

```java
@Test
void registrations_should_batch_load_patients_and_doctors_for_current_page_only() {
    PatientMapper patientMapper = mock(PatientMapper.class);
    OutpatientRegistrationMapper registrationMapper = mock(OutpatientRegistrationMapper.class);
    DoctorProfileMapper doctorMapper = mock(DoctorProfileMapper.class);
    AuditService auditService = mock(AuditService.class);
    PatientService service = new PatientService(patientMapper, registrationMapper, doctorMapper, auditService);

    Page<OutpatientRegistration> page = new Page<>(1, 10, 1);
    OutpatientRegistration reg = new OutpatientRegistration();
    reg.setId(101L);
    reg.setPatientId(201L);
    reg.setDoctorId(301L);
    reg.setDepartment("儿科");
    reg.setStatus("待诊");
    reg.setPaid("Y");
    reg.setScheduleDate("2026-06-28");
    reg.setFee(BigDecimal.TEN);
    reg.setCreatedAt(LocalDateTime.now());
    page.setRecords(List.of(reg));

    when(registrationMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenAnswer(invocation -> {
        Page<OutpatientRegistration> target = invocation.getArgument(0);
        target.setTotal(page.getTotal());
        target.setRecords(page.getRecords());
        return target;
    });
    when(patientMapper.selectBatchIds(List.of(201L))).thenReturn(List.of(patient(201L, "李雷")));
    when(doctorMapper.selectBatchIds(List.of(301L))).thenReturn(List.of(doctor(301L, "王医生")));

    PageResponse<Map<String, Object>> response = service.registrations(null, "待诊", 1, 10);

    assertThat(response.records()).singleElement().satisfies(item -> {
        assertThat(item.get("patientName")).isEqualTo("李雷");
        assertThat(item.get("doctorName")).isEqualTo("王医生");
    });
    verify(patientMapper).selectBatchIds(List.of(201L));
    verify(doctorMapper).selectBatchIds(List.of(301L));
    verify(patientMapper, never()).selectById(anyLong());
    verify(doctorMapper, never()).selectById(anyLong());
}
```

- [ ] **Step 4: Run test to verify it fails**

Run: `mvn -pl neusoft-his-system/his-service -am -Dtest=PatientServicePaginationTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: FAIL because current implementation calls `selectById` per row.

- [ ] **Step 5: Write the failing audit pagination test**

```java
@Test
void list_should_return_page_response_instead_of_full_list() {
    SysAuditLogMapper mapper = mock(SysAuditLogMapper.class);
    DbAuditServiceImpl service = new DbAuditServiceImpl(mapper);

    when(mapper.selectAuditPage(any(Page.class))).thenReturn(List.of(
            auditLog(1L, "admin", "LOGIN", "登录系统", LocalDateTime.of(2026, 6, 28, 10, 0))
    ));
    when(mapper.countAuditPage()).thenReturn(1L);

    PageResponse<AuditLogEntry> response = service.page(1, 10);

    assertThat(response.total()).isEqualTo(1);
    assertThat(response.records()).hasSize(1);
    verify(mapper).selectAuditPage(any(Page.class));
}
```

- [ ] **Step 6: Run test to verify it fails**

Run: `mvn -pl neusoft-his-system/his-common -am -Dtest=DbAuditServicePaginationTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: FAIL because `page(...)` and mapper pagination methods do not exist yet.

### Task 2: Implement Backend True Pagination And Query Pushdown

**Files:**
- Modify: `neusoft-his-system/his-service/src/main/java/com/neusoft/his/service/doctor/DoctorWorkstationService.java`
- Modify: `neusoft-his-system/his-service/src/main/java/com/neusoft/his/service/patient/PatientService.java`
- Modify: `neusoft-his-system/his-common/src/main/java/com/neusoft/his/common/audit/AuditService.java`
- Modify: `neusoft-his-system/his-common/src/main/java/com/neusoft/his/common/audit/DbAuditServiceImpl.java`
- Modify: `neusoft-his-system/his-api/src/main/java/com/neusoft/his/api/controller/AuditController.java`
- Modify: `neusoft-his-system/his-dal/src/main/java/com/neusoft/his/dal/mapper/DoctorScheduleMapper.java`
- Create: `neusoft-his-system/his-dal/src/main/resources/mapper/DoctorScheduleMapper.xml`
- Modify: `neusoft-his-system/his-dal/src/main/java/com/neusoft/his/dal/mapper/SysAuditLogMapper.java`
- Create: `neusoft-his-system/his-dal/src/main/resources/mapper/SysAuditLogMapper.xml`

- [ ] **Step 1: Add mapper contracts for paged schedule and audit queries**

```java
List<DoctorScheduleView> selectSchedulePage(Page<?> page,
                                            @Param("department") String department,
                                            @Param("doctorName") String doctorName,
                                            @Param("date") String date);

long countSchedulePage(@Param("department") String department,
                       @Param("doctorName") String doctorName,
                       @Param("date") String date);
```

```java
List<SysAuditLog> selectAuditPage(Page<?> page);

long countAuditPage();
```

- [ ] **Step 2: Implement XML SQL for schedule join and remaining quota aggregation**

```xml
<select id="selectSchedulePage" resultType="com.neusoft.his.service.dto.DoctorScheduleView">
  SELECT
    ds.id,
    ds.doctor_id AS doctorId,
    ds.schedule_date AS scheduleDate,
    ds.shift,
    dp.department,
    dp.name AS doctorName,
    dp.name AS name,
    dp.title,
    CASE WHEN ds.status = 0 THEN '停诊' ELSE ds.shift END AS attendanceStatus,
    ds.level,
    COALESCE(ds.registration_limit, 0) AS `limit`,
    GREATEST(COALESCE(ds.registration_limit, 0) - COALESCE(reg.used_count, 0), 0) AS remain,
    ds.status
  FROM doctor_schedule ds
  LEFT JOIN doctor_profile dp ON dp.id = ds.doctor_id
  LEFT JOIN (
    SELECT doctor_id, schedule_date, COUNT(*) AS used_count
    FROM outpatient_registration
    GROUP BY doctor_id, schedule_date
  ) reg ON reg.doctor_id = ds.doctor_id AND reg.schedule_date = ds.schedule_date
  <where>
    <if test="date != null and date != ''">AND ds.schedule_date = #{date}</if>
    <if test="department != null and department != ''">AND dp.department = #{department}</if>
    <if test="doctorName != null and doctorName != ''">AND dp.name LIKE CONCAT('%', #{doctorName}, '%')</if>
  </where>
  ORDER BY ds.schedule_date DESC, ds.shift ASC, ds.created_at DESC
</select>
```

- [ ] **Step 3: Implement service methods against the new paged mapper APIs**

```java
public PageResponse<DoctorScheduleView> scheduleQuery(String department, String doctorName, String date,
                                                      long page, long size) {
    long safePage = Math.max(page, 1);
    long safeSize = Math.max(size, 1);
    Page<Object> pageParam = new Page<>(safePage, safeSize);
    List<DoctorScheduleView> records = scheduleMapper.selectSchedulePage(pageParam, department, doctorName, date);
    long total = scheduleMapper.countSchedulePage(department, doctorName, date);
    return new PageResponse<>(safePage, safeSize, total, records);
}
```

```java
public PageResponse<AuditLogEntry> page(long page, long size) {
    Page<Object> pageParam = new Page<>(Math.max(page, 1), Math.max(size, 1));
    List<AuditLogEntry> records = auditMapper.selectAuditPage(pageParam).stream()
            .map(log -> new AuditLogEntry(log.getTime(), log.getUsername(), log.getOperation(), log.getDetail()))
            .toList();
    return new PageResponse<>(pageParam.getCurrent(), pageParam.getSize(), auditMapper.countAuditPage(), records);
}
```

- [ ] **Step 4: Replace registration per-row lookups with batched current-page loading**

```java
List<Long> patientIds = pageParam.getRecords().stream()
        .map(OutpatientRegistration::getPatientId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
Map<Long, Patient> patients = patientIds.isEmpty() ? Map.of() :
        patientMapper.selectBatchIds(patientIds).stream()
                .collect(Collectors.toMap(Patient::getId, Function.identity()));
```

- [ ] **Step 5: Run focused tests to verify they pass**

Run: `mvn -pl neusoft-his-system/his-common,neusoft-his-system/his-service -am -Dtest=DoctorWorkstationServicePaginationTest,PatientServicePaginationTest,DbAuditServicePaginationTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: PASS for the three new test classes.

### Task 3: Extend Finance, Pharmacy, Dashboard, And Analytics Query Surfaces

**Files:**
- Modify: `neusoft-his-system/his-service/src/main/java/com/neusoft/his/service/finance/FinanceService.java`
- Modify: `neusoft-his-system/his-api/src/main/java/com/neusoft/his/api/controller/FinanceController.java`
- Modify: `neusoft-his-system/his-service/src/main/java/com/neusoft/his/service/pharmacy/PharmacyService.java`
- Modify: `neusoft-his-system/his-api/src/main/java/com/neusoft/his/api/controller/PharmacyController.java`
- Modify: `neusoft-his-system/his-service/src/main/java/com/neusoft/his/service/analytics/AnalyticsService.java`

- [ ] **Step 1: Write failing tests for finance/pharmacy searchable pagination and dashboard aggregation**

```java
assertThat(service.bills("PAID", "BL12", "李", 1, 10).records()).allMatch(item -> "PAID".equals(item.getStatus()));
assertThat(service.inventory("DRUG", "阿莫", true, 1, 10).records()).allMatch(item -> item.getStock() <= item.getWarningThreshold());
assertThat(service.dashboardMetrics().get("todayIncome")).isEqualTo(BigDecimal.valueOf(100));
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -pl neusoft-his-system/his-service -am -Dtest=FinanceServicePaginationTest,PharmacyServicePaginationTest,AnalyticsServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: FAIL because current method signatures and aggregation helpers are missing.

- [ ] **Step 3: Add backend query parameters and aggregation helpers**

```java
public PageResponse<BillingRecord> bills(String status, String billNoKeyword, String patientKeyword, long page, long size)
public PageResponse<DrugCatalog> inventory(String codeKeyword, String nameKeyword, Boolean warningOnly, long page, long size)
public Map<String, Object> dashboardMetrics()
```

- [ ] **Step 4: Push searchable conditions into MyBatis-Plus wrappers**

```java
if (StringUtils.isNotBlank(billNoKeyword)) {
    query.and(wrapper -> wrapper.like("CAST(id AS CHAR)", billNoKeyword));
}
if (Boolean.TRUE.equals(warningOnly)) {
    query.apply("stock <= warning_threshold");
}
```

- [ ] **Step 5: Run focused tests to verify they pass**

Run: `mvn -pl neusoft-his-system/his-service -am -Dtest=FinanceServicePaginationTest,PharmacyServicePaginationTest,AnalyticsServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: PASS.

### Task 4: Add Targeted SQL Indexes For Real Query Paths

**Files:**
- Modify: `neusoft-his-system/his-dal/src/main/resources/schema.sql`

- [ ] **Step 1: Write a failing schema assertion test for required indexes**

```java
assertThat(schemaSql).contains("idx_outpatient_registration_status_created");
assertThat(schemaSql).contains("idx_doctor_schedule_date_doctor_shift");
assertThat(schemaSql).contains("idx_billing_record_status_created");
assertThat(schemaSql).contains("idx_sys_audit_log_time");
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl neusoft-his-system/his-dal -am -Dtest=SchemaIndexDefinitionTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: FAIL because the index DDL is missing.

- [ ] **Step 3: Add only the targeted indexes used by the new query paths**

```sql
CREATE INDEX idx_outpatient_registration_status_created ON outpatient_registration(status, created_at);
CREATE INDEX idx_outpatient_registration_doctor_date ON outpatient_registration(doctor_id, schedule_date);
CREATE INDEX idx_doctor_schedule_date_doctor_shift ON doctor_schedule(schedule_date, doctor_id, shift);
CREATE INDEX idx_patient_name_no_idcard ON patient(name, patient_no, id_card);
CREATE INDEX idx_billing_record_status_created ON billing_record(status, created_at);
CREATE INDEX idx_drug_catalog_name_stock ON drug_catalog(name, stock, warning_threshold);
CREATE INDEX idx_sys_audit_log_time ON sys_audit_log(time);
CREATE INDEX idx_financial_transaction_created_direction ON financial_transaction(created_at, direction);
```

- [ ] **Step 4: Run the schema test to verify it passes**

Run: `mvn -pl neusoft-his-system/his-dal -am -Dtest=SchemaIndexDefinitionTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: PASS.

### Task 5: Convert Frontend Pages To Shared Server-Side Pagination Behavior

**Files:**
- Modify: `his-frontend/src/utils/request.js`
- Modify: `his-frontend/src/views/doctor/schedule.vue`
- Modify: `his-frontend/src/views/patient/registration.vue`
- Modify: `his-frontend/src/views/patient/tracking.vue`
- Modify: `his-frontend/src/views/finance/index.vue`
- Modify: `his-frontend/src/views/pharmacy/index.vue`
- Modify: `his-frontend/src/views/system/index.vue`
- Modify: `his-frontend/src/views/dashboard/index.vue`

- [ ] **Step 1: Add failing frontend tests or, if no test harness exists, add a temporary verification checklist in code comments before implementation**

```js
// Verification targets:
// 1. Query changes reset current page to 1.
// 2. Table data binds only to res.records.
// 3. Pagination total binds to res.total.
// 4. No request uses size 100/200/500 for ordinary list pages.
```

- [ ] **Step 2: Increase request timeout before wider pagination rollout**

```js
const request = axios.create({
  baseURL: "",
  timeout: 30000,
});
```

- [ ] **Step 3: For each list page, introduce shared page state and pagination handlers**

```js
const pageState = reactive({ page: 1, size: 10, total: 0 });

const handleSearch = () => {
  pageState.page = 1;
  fetchSchedules();
};
```

- [ ] **Step 4: Remove local `.filter()` over oversized results and pass query params to backend**

```js
const res = await request.get("/api/finance/bills", {
  params: {
    status: "PRICED",
    keyword: chargeQuery.patientNo || undefined,
    patientKeyword: chargeQuery.patientName || undefined,
    page: chargePage.page,
    size: chargePage.size,
  },
});
pendingBills.value = (res.records || []).map(mapBill);
chargePage.total = res.total || 0;
```

- [ ] **Step 5: Add `el-pagination` controls where missing and bind `current-page`, `page-size`, and `total`**

```vue
<el-pagination
  v-model:current-page="pageState.page"
  v-model:page-size="pageState.size"
  :page-sizes="[10, 20, 50]"
  layout="total, sizes, prev, pager, next, jumper"
  :total="pageState.total"
  @current-change="fetchData"
  @size-change="handlePageSizeChange"
/>
```

- [ ] **Step 6: Run frontend build to verify the pagination refactor compiles**

Run: `npm run build`
Expected: PASS in `his-frontend`.

### Task 6: Full Verification And Manual Spot Checks

**Files:**
- No code changes required unless verification finds regressions

- [ ] **Step 1: Run backend module tests covering the new pagination/query logic**

Run: `mvn test -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS or only known non-blocking warnings.

- [ ] **Step 2: Run frontend production build**

Run: `npm run build`
Expected: PASS.

- [ ] **Step 3: Manually spot-check key endpoints**

Run:
```bash
curl -H "Authorization: Bearer <token>" "http://localhost:8080/api/doctors/schedules?page=1&size=10"
curl -H "Authorization: Bearer <token>" "http://localhost:8080/api/patients/registrations?page=1&size=10"
curl -H "Authorization: Bearer <token>" "http://localhost:8080/api/audit/logs?page=1&size=10"
```
Expected: All return paged payloads with `page`, `size`, `total`, and `records`.

- [ ] **Step 4: Manually spot-check frontend pages**

Verify:
- `doctor/schedule` query resets to page 1 and switches pages without fetching 100 rows.
- `patient/registration` and `patient/tracking` no longer perform local list filtering over oversized data.
- `finance`, `pharmacy`, and `system` pages show correct totals and preserve existing operations.

- [ ] **Step 5: Commit**

```bash
git add docs/superpowers/plans/2026-06-28-his-full-pagination-performance.md \
  neusoft-his-system his-frontend
git commit -m "feat: implement true pagination and query performance fixes"
```
