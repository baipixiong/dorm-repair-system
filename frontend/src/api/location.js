import request from './request'

export const getLocations = () => request.get('/locations')

export const addBuilding = (data) => request.post('/admin/locations/buildings', data)

export const addRoom = (data) => request.post('/admin/locations/rooms', data)
