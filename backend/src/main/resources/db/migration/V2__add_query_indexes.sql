-- V2 常用查询的索引
-- 单件在售检查、当前商品、历史商品都按状态查商品，历史商品再按发布时间排序
CREATE INDEX idx_product_status_time ON product (status, create_time);

-- 排队、意向人列表都按商品和意向状态查
CREATE INDEX idx_intent_product_status ON purchase_intent (product_id, status);
