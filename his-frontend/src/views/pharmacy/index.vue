<template>
  <div>
    <el-row :gutter="20">
      <el-col :span="16">
        <el-card>
          <template #header>
            <div
              style="
                display: flex;
                justify-content: space-between;
                align-items: center;
              "
            >
              <span>药品库存台账</span>
              <el-button
                type="success"
                icon="Plus"
                @click="dialogVisible = true"
                >新增药品</el-button
              >
            </div>
          </template>

          <el-table :data="inventoryList" v-loading="loading" border>
            <el-table-column prop="code" label="药品编码" width="120" />
            <el-table-column prop="name" label="药品名称" />
            <el-table-column prop="unit" label="单位" width="80" />
            <el-table-column prop="price" label="单价(元)" width="100" />
            <el-table-column prop="stock" label="当前库存" width="100">
              <template #default="scope">
                <el-tag
                  :type="
                    scope.row.stock < scope.row.warningThreshold
                      ? 'danger'
                      : 'success'
                  "
                >
                  {{ scope.row.stock }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="scope">
                <el-button
                  size="small"
                  type="primary"
                  link
                  @click="handleInbound(scope.row)"
                  >入库</el-button
                >
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="hover" header="库存预警">
          <el-alert
            v-for="item in warningList"
            :key="item.id"
            :title="`${item.name} 库存告急 (仅剩 ${item.stock} ${item.unit})`"
            type="error"
            show-icon
            :closable="false"
            style="margin-bottom: 10px"
          />
          <el-empty
            v-if="warningList.length === 0"
            description="暂无库存预警"
          />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="dialogVisible" title="新增药品目录" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="药品编码"
          ><el-input v-model="form.code"
        /></el-form-item>
        <el-form-item label="药品名称"
          ><el-input v-model="form.name"
        /></el-form-item>
        <el-form-item label="单位"
          ><el-input v-model="form.unit" placeholder="如：盒、瓶"
        /></el-form-item>
        <el-form-item label="零售价"
          ><el-input-number v-model="form.price" :min="0" :precision="2"
        /></el-form-item>
        <el-form-item label="初始库存"
          ><el-input-number v-model="form.stock" :min="0"
        /></el-form-item>
        <el-form-item label="预警阈值"
          ><el-input-number v-model="form.warningThreshold" :min="1"
        /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitDrug">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from "vue";
import request from "../../utils/request";
import { ElMessage, ElMessageBox } from "element-plus";

const loading = ref(false);
const inventoryList = ref([]);
const warningList = ref([]);
const dialogVisible = ref(false);

const form = reactive({
  code: "",
  name: "",
  unit: "盒",
  price: 0,
  stock: 100,
  warningThreshold: 20,
});

// 获取库存和预警数据
const fetchData = async () => {
  loading.value = true;
  try {
    const res = await request.get("/api/pharmacy/inventory");
    inventoryList.value = res.records || [];

    const warnRes = await request.get("/api/pharmacy/inventory/warnings");
    warningList.value = warnRes || [];
  } finally {
    loading.value = false;
  }
};

// 提交新增药品
const submitDrug = async () => {
  await request.post("/api/pharmacy/drugs", form);
  ElMessage.success("药品添加成功");
  dialogVisible.value = false;
  fetchData();
};

// 快捷入库弹窗
const handleInbound = (row) => {
  ElMessageBox.prompt(`请输入【${row.name}】的入库数量`, "药品入库", {
    confirmButtonText: "确认入库",
    cancelButtonText: "取消",
    inputPattern: /^[1-9]\d*$/,
    inputErrorMessage: "请输入大于0的正整数",
  })
    .then(async ({ value }) => {
      await request.post(
        `/api/pharmacy/drugs/${row.id}/inbound?quantity=${value}`,
      );
      ElMessage.success("入库成功");
      fetchData();
    })
    .catch(() => {});
};

onMounted(() => {
  fetchData();
});
</script>
