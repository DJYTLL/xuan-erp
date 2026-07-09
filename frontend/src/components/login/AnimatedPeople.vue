<template>
  <div
    ref="stageRef"
    class="login-people"
    @mousemove="handleMouseMove"
    @mouseleave="resetFace"
  >
    <div class="login-person person-purple">
      <div class="person-eyes">
        <span class="eye"><span class="pupil" /></span>
        <span class="eye"><span class="pupil" /></span>
      </div>
    </div>
    <div class="login-person person-dark">
      <div class="person-eyes">
        <span class="eye"><span class="pupil" /></span>
        <span class="eye"><span class="pupil" /></span>
      </div>
    </div>
    <div class="login-person person-orange">
      <div class="person-eyes">
        <span class="eye" />
        <span class="eye" />
      </div>
    </div>
    <div class="login-person person-yellow">
      <div class="person-eyes">
        <span class="eye" />
        <span class="eye" />
      </div>
      <span class="mouth" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

const stageRef = ref<HTMLElement | null>(null);

function clamp(value: number, min: number, max: number) {
  return Math.max(min, Math.min(max, value));
}

function setFace(x: number, y: number) {
  const stage = stageRef.value;
  if (!stage) {
    return;
  }
  stage.style.setProperty('--pupil-x', `${x}px`);
  stage.style.setProperty('--pupil-y', `${y}px`);
  stage.style.setProperty('--face-x', `${x * 0.35}px`);
  stage.style.setProperty('--face-y', `${y * 0.35}px`);
}

function handleMouseMove(event: MouseEvent) {
  const stage = stageRef.value;
  if (!stage) {
    return;
  }
  const rect = stage.getBoundingClientRect();
  const centerX = rect.left + rect.width / 2;
  const centerY = rect.top + rect.height / 2;
  setFace(
    clamp((event.clientX - centerX) / 55, -5, 5),
    clamp((event.clientY - centerY) / 55, -5, 5),
  );
}

function resetFace() {
  setFace(0, 0);
}
</script>
