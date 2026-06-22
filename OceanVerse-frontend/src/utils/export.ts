/**
 * 前端导出工具 — PDF / PNG / Blob 下载
 */
import * as echarts from 'echarts'
import jsPDF from 'jspdf'
import html2canvas from 'html2canvas'

/**
 * 通用 Blob 文件下载
 */
export function downloadBlob(blob: Blob, filename: string): void {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

/**
 * 从 axios response 中提取 Blob 并下载
 * http.ts 中 blob 响应返回原始 response 对象
 */
export function downloadFromResponse(response: any, fallbackName: string): void {
  // 尝试从 Content-Disposition 头获取文件名
  const disposition = response.headers?.['content-disposition'] || ''
  let filename = fallbackName
  const match = disposition.match(/filename=([^;]+)/)
  if (match) {
    filename = decodeURIComponent(match[1].trim().replaceAll('%20', ' '))
  }

  const blob = response.data instanceof Blob ? response.data : new Blob([response.data])
  downloadBlob(blob, filename)
}

/**
 * 导出 ECharts 图表为 PNG
 */
export function exportChartAsPNG(chartInstance: echarts.ECharts, filename: string): void {
  const url = chartInstance.getDataURL({
    type: 'png',
    pixelRatio: 2,
    backgroundColor: '#fff'
  })
  const link = document.createElement('a')
  link.href = url
  link.download = `${filename}.png`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

/**
 * 将多个 ECharts 图表导出为 PDF 报表
 * @param charts 图表实例数组
 * @param title 报表标题
 * @param filename 文件名（不含扩展名）
 */
export async function exportChartsAsPDF(
  charts: echarts.ECharts[],
  title: string,
  filename: string
): Promise<void> {
  const pdf = new jsPDF({ orientation: 'landscape', unit: 'mm', format: 'a4' })
  const pageWidth = pdf.internal.pageSize.getWidth()
  const pageHeight = pdf.internal.pageSize.getHeight()
  const margin = 15

  // 标题
  pdf.setFontSize(18)
  pdf.text(title, pageWidth / 2, margin + 5, { align: 'center' })

  let yOffset = margin + 15

  for (let i = 0; i < charts.length; i++) {
    const chart = charts[i]
    const imgUrl = chart.getDataURL({
      type: 'png',
      pixelRatio: 2,
      backgroundColor: '#fff'
    })

    // 将图片转为 canvas 获取尺寸
    const imgCanvas = await html2canvas(imgToDom(imgUrl), { scale: 1 })
    const imgWidth = pageWidth - margin * 2
    const imgHeight = (imgCanvas.height / imgCanvas.width) * imgWidth

    // 如果超出页面，新增一页
    if (yOffset + imgHeight > pageHeight - margin) {
      pdf.addPage()
      yOffset = margin
    }

    pdf.addImage(imgUrl, 'PNG', margin, yOffset, imgWidth, imgHeight)
    yOffset += imgHeight + 8
  }

  pdf.save(`${filename}.pdf`)
}

/**
 * 导出 HTML 元素为 PDF（用于截取整个区域，含表格等）
 */
export async function exportElementAsPDF(
  element: HTMLElement,
  title: string,
  filename: string
): Promise<void> {
  const canvas = await html2canvas(element, {
    scale: 2,
    backgroundColor: '#ffffff',
    useCORS: true
  })

  const imgData = canvas.toDataURL('image/png')
  const pdf = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' })
  const pageWidth = pdf.internal.pageSize.getWidth()
  const pageHeight = pdf.internal.pageSize.getHeight()
  const imgWidth = pageWidth
  const imgHeight = (canvas.height / canvas.width) * imgWidth

  // 标题
  pdf.setFontSize(16)
  pdf.text(title, pageWidth / 2, 12, { align: 'center' })

  let heightLeft = imgHeight
  let position = 18

  pdf.addImage(imgData, 'PNG', 0, position, imgWidth, imgHeight)
  heightLeft -= (pageHeight - position)

  // 多页处理
  while (heightLeft > 0) {
    position = heightLeft - imgHeight + 18
    pdf.addPage()
    pdf.addImage(imgData, 'PNG', 0, position, imgWidth, imgHeight)
    heightLeft -= pageHeight
  }

  pdf.save(`${filename}.pdf`)
}

// ==================== 辅助函数 ====================

function imgToDom(src: string): HTMLImageElement {
  const img = new Image()
  img.src = src
  img.style.width = '100%'
  return img
}
