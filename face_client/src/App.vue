<script setup lang="ts"></script>

<template>
  <!-- 顶部固定标题 -->
  <div
    class="fixed top-0 left-0 w-full h-16 bg-white shadow z-50 flex items-center justify-center"
  >
    <h1 class="text-xl font-bold text-gray-800">scenery</h1>
  </div>

  <!-- 中间滚动内容区域：全高减去顶部(16) + 底部(12) = 剩下内容区高度 -->
  <div
    class="fixed top-16 bottom-12 left-0 right-0 overflow-y-auto bg-white"
  >
    <RouterView />
  </div>

  <!-- 底部固定信息栏 -->
  <div
    class="fixed bottom-0 left-0 w-full h-12 bg-gray-100 shadow-inner z-50 flex items-center justify-center text-sm text-gray-600"
  >
    我的个人网站信息 © 2025
  </div>
</template>

<style scoped></style>
