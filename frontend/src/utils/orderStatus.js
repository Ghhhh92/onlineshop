export const ORDER_STATUS = {
  PENDING: { text: '客户下单', type: 'primary' },
  CONFIRMED: { text: '商家确认', type: 'warning' },
  PREPARED: { text: '备货完成', type: 'warning' },
  SHIPPED: { text: '开始发货', type: 'info' },
  COMPLETED: { text: '交易完成', type: 'success' },
  CANCELED: { text: '已取消', type: 'info' },
}

export function orderStatusText(status) {
  return ORDER_STATUS[status]?.text || status || '未知'
}

export function orderStatusType(status) {
  return ORDER_STATUS[status]?.type || 'info'
}

export function customerCanCancel(status) {
  return ['PENDING', 'CONFIRMED', 'PREPARED'].includes(status)
}

export function sellerCanCancel(status) {
  return ['PENDING', 'CONFIRMED', 'PREPARED', 'SHIPPED'].includes(status)
}