(function() {
  var style = getComputedStyle(document.documentElement);
  var accent = style.getPropertyValue('--accent').trim();
  var accent2 = style.getPropertyValue('--accent2').trim();
  var ink = style.getPropertyValue('--ink').trim();
  var muted = style.getPropertyValue('--muted').trim();
  var rule = style.getPropertyValue('--rule').trim();
  var bg2 = style.getPropertyValue('--bg2').trim();
  var success = style.getPropertyValue('--success').trim();
  var danger = style.getPropertyValue('--danger').trim();
  var warning = style.getPropertyValue('--warning').trim();

  // --- Chart 1: Problem Status Comparison (Radar) ---
  var chart1 = echarts.init(document.getElementById('chart-radar'), null, { renderer: 'svg' });
  chart1.setOption({
    tooltip: { appendToBody: true },
    legend: { bottom: 0, textStyle: { color: muted, fontSize: 12 } },
    radar: {
      indicator: [
        { name: '安全', max: 10 },
        { name: '后端架构', max: 10 },
        { name: '前端工程', max: 10 },
        { name: '数据库', max: 10 },
        { name: 'DevOps', max: 10 }
      ],
      axisName: { color: muted, fontSize: 12 },
      splitArea: { show: false },
      splitLine: { lineStyle: { color: rule } },
      axisLine: { lineStyle: { color: rule } }
    },
    series: [{
      type: 'radar',
      data: [
        {
          value: [7, 8, 9, 4, 6],
          name: '首轮评估（问题数）',
          lineStyle: { color: danger },
          areaStyle: { color: danger + '30' },
          itemStyle: { color: danger }
        },
        {
          value: [4, 5, 9, 2, 4],
          name: '本轮评估（问题数）',
          lineStyle: { color: success },
          areaStyle: { color: success + '30' },
          itemStyle: { color: success }
        }
      ]
    }],
    animation: false
  });
  window.addEventListener('resize', function() { chart1.resize(); });

  // --- Chart 2: Issue Resolution Progress (Bar) ---
  var chart2 = echarts.init(document.getElementById('chart-progress'), null, { renderer: 'svg' });
  chart2.setOption({
    tooltip: { appendToBody: true, trigger: 'axis' },
    legend: { bottom: 0, textStyle: { color: muted, fontSize: 12 } },
    grid: { top: 20, right: 20, bottom: 50, left: 100 },
    xAxis: { type: 'value', max: 12, axisLine: { lineStyle: { color: rule } }, axisLabel: { color: muted }, splitLine: { lineStyle: { color: rule } } },
    yAxis: {
      type: 'category',
      data: ['安全', '后端架构', '前端工程', '数据库', 'DevOps'],
      axisLine: { lineStyle: { color: rule } },
      axisLabel: { color: ink, fontSize: 12 }
    },
    series: [
      {
        name: '已解决',
        type: 'bar',
        stack: 'total',
        data: [3, 3, 0, 2, 2],
        itemStyle: { color: success },
        barWidth: 20
      },
      {
        name: '新增发现',
        type: 'bar',
        stack: 'total',
        data: [0, 3, 0, 0, 0],
        itemStyle: { color: warning }
      },
      {
        name: '仍存在',
        type: 'bar',
        stack: 'total',
        data: [4, 5, 9, 2, 4],
        itemStyle: { color: danger + '60' }
      }
    ],
    animation: false
  });
  window.addEventListener('resize', function() { chart2.resize(); });

  // --- Chart 3: Severity Distribution (Pie) ---
  var chart3 = echarts.init(document.getElementById('chart-severity'), null, { renderer: 'svg' });
  chart3.setOption({
    tooltip: { appendToBody: true, trigger: 'item', formatter: '{b}: {c} 项 ({d}%)' },
    legend: { bottom: 0, textStyle: { color: muted, fontSize: 12 } },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: true,
      label: { show: true, color: ink, formatter: '{b}\n{c}项', fontSize: 11 },
      labelLine: { lineStyle: { color: rule } },
      data: [
        { value: 6, name: '严重', itemStyle: { color: danger } },
        { value: 10, name: '高', itemStyle: { color: warning } },
        { value: 10, name: '中', itemStyle: { color: accent } },
        { value: 6, name: '低', itemStyle: { color: success } }
      ]
    }],
    animation: false
  });
  window.addEventListener('resize', function() { chart3.resize(); });
})();