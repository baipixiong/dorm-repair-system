import request from './request'

export const getLocations = () => request.get('/locations')

export const addBuilding = (data) => request.post('/admin/locations/buildings', data)
