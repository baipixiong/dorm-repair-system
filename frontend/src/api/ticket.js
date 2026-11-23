import request from './request'

export const getTickets = (params) => request.get('/tickets', { params })

export const createTicket = (data) => request.post('/tickets', data)

export const getTicket = (id) => request.get(`/tickets/${id}`)

export const ticketAction = (id, data) => request.post(`/tickets/${id}/actions`, data)

export const evaluateTicket = (id, data) => request.post(`/tickets/${id}/evaluation`, data)

export const uploadAttachment = (id, file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post(`/tickets/${id}/attachments`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export async function fetchAttachmentUrl(id) {
  const response = await request.get(`/attachments/${id}`, { responseType: 'blob' })
  return URL.createObjectURL(response.data)
}

export async function exportTickets(params) {
  const response = await request.get('/admin/export', { params, responseType: 'blob' })
  const url = URL.createObjectURL(response.data)
  const link = document.createElement('a')
  link.href = url
  link.download = `工单列表_${new Date().toISOString().slice(0, 10)}.csv`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}
