<template>
  <div>
    <div class="page-header"><h2>物种分布地图</h2><p>全球海洋物种分布可视化</p></div>
    <el-card>
      <div ref="mapContainer" style="height:600px;border-radius:12px;overflow:hidden"></div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import L from 'leaflet'

const mapContainer = ref<HTMLElement>()

onMounted(() => {
  if (!mapContainer.value) return
  const map = L.map(mapContainer.value).setView([20, 110], 3)
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap contributors'
  }).addTo(map)

  // 示例标记点
  const markers = [
    { lat: 18.25, lng: 109.5, name: '海南珊瑚礁', species: '珊瑚礁生态系统' },
    { lat: 22.27, lng: 114.16, name: '香港水域', species: '中华白海豚' },
    { lat: 30.5, lng: 114.3, name: '长江流域', species: '长江江豚' }
  ]
  markers.forEach(m => {
    L.marker([m.lat, m.lng])
      .addTo(map)
      .bindPopup(`<b>${m.name}</b><br>${m.species}`)
  })
})
</script>
