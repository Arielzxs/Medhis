import request from "./request";

export const fetchDepartmentOptions = async ({ includePending = false, enabledOnly = true } = {}) => {
  const data = await request.get("/api/departments", {
    params: { enabledOnly },
  });
  const names = (data || []).map((item) => item.name).filter(Boolean);
  return includePending ? ["待分配", ...names] : names;
};
