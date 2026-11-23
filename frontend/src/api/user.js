import request from './request'

export const getUsers = (params) => request.get('/users', { params })

export const createUser = (data) => request.post('/admin/users', data)

export const updateUserStatus = (id, status) => request.put(`/admin/users/${id}/status`, { status })

export const getTechnicians = () => request.get('/technicians')

export const getAuditLogs = () => request.get('/admin/audit')
