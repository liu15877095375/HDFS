<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script setup>
// 无需额外逻辑，路由会自动根据 URL 渲染对应页面
</script>

<style>
/* 全局重置，保证页面全屏且无默认边距 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html,
body,
#app {
  width: 100%;
  height: 100%;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Microsoft YaHei', Arial, sans-serif;
}
</style>
