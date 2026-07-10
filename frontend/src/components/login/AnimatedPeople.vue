<template>
  <div class="animated-characters" aria-hidden="true">
    <div ref="purpleRef" class="character character-purple" :style="purpleStyle">
      <div class="character-eyes purple-eyes" :style="purpleEyesStyle">
        <span class="eyeball purple-eye" :class="{ blinking: isPurpleBlinking }">
          <span class="eyeball-pupil" :style="purplePupilStyle" />
        </span>
        <span class="eyeball purple-eye" :class="{ blinking: isPurpleBlinking }">
          <span class="eyeball-pupil" :style="purplePupilStyle" />
        </span>
      </div>
    </div>

    <div ref="blackRef" class="character character-black" :style="blackStyle">
      <div class="character-eyes black-eyes" :style="blackEyesStyle">
        <span class="eyeball black-eye" :class="{ blinking: isBlackBlinking }">
          <span class="eyeball-pupil black-pupil" :style="blackPupilStyle" />
        </span>
        <span class="eyeball black-eye" :class="{ blinking: isBlackBlinking }">
          <span class="eyeball-pupil black-pupil" :style="blackPupilStyle" />
        </span>
      </div>
    </div>

    <div ref="orangeRef" class="character character-orange" :style="orangeStyle">
      <div class="character-eyes orange-eyes" :style="orangeEyesStyle">
        <span class="dot-eye" :style="orangePupilStyle" />
        <span class="dot-eye" :style="orangePupilStyle" />
      </div>
    </div>

    <div ref="yellowRef" class="character character-yellow" :style="yellowStyle">
      <div class="character-eyes yellow-eyes" :style="yellowEyesStyle">
        <span class="dot-eye" :style="yellowPupilStyle" />
        <span class="dot-eye" :style="yellowPupilStyle" />
      </div>
      <span class="yellow-mouth" :style="yellowMouthStyle" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';

const props = withDefaults(defineProps<{
  isTyping?: boolean;
  showPassword?: boolean;
  passwordLength?: number;
}>(), {
  isTyping: false,
  showPassword: false,
  passwordLength: 0,
});

type CharacterPosition = {
  faceX: number;
  faceY: number;
  bodySkew: number;
};

const mouseX = ref(0);
const mouseY = ref(0);
const isPurpleBlinking = ref(false);
const isBlackBlinking = ref(false);
const isLookingAtEachOther = ref(false);
const isPurplePeeking = ref(false);

const purpleRef = ref<HTMLElement | null>(null);
const blackRef = ref<HTMLElement | null>(null);
const orangeRef = ref<HTMLElement | null>(null);
const yellowRef = ref<HTMLElement | null>(null);

let purpleBlinkTimer: number | undefined;
let purpleBlinkOffTimer: number | undefined;
let blackBlinkTimer: number | undefined;
let blackBlinkOffTimer: number | undefined;
let lookingTimer: number | undefined;
let peekTimer: number | undefined;
let peekOffTimer: number | undefined;

function clamp(value: number, min: number, max: number) {
  return Math.max(min, Math.min(max, value));
}

function handleMouseMove(event: MouseEvent) {
  mouseX.value = event.clientX;
  mouseY.value = event.clientY;
}

function calculatePosition(element: HTMLElement | null): CharacterPosition {
  if (!element) {
    return { faceX: 0, faceY: 0, bodySkew: 0 };
  }

  const rect = element.getBoundingClientRect();
  const centerX = rect.left + rect.width / 2;
  const centerY = rect.top + rect.height / 3;
  const deltaX = mouseX.value - centerX;
  const deltaY = mouseY.value - centerY;

  return {
    faceX: clamp(deltaX / 20, -15, 15),
    faceY: clamp(deltaY / 30, -10, 10),
    bodySkew: clamp(-deltaX / 120, -6, 6),
  };
}

function calculatePupilOffset(element: HTMLElement | null, maxDistance: number) {
  if (!element) {
    return { x: 0, y: 0 };
  }

  const rect = element.getBoundingClientRect();
  const centerX = rect.left + rect.width / 2;
  const centerY = rect.top + rect.height / 4;
  const deltaX = mouseX.value - centerX;
  const deltaY = mouseY.value - centerY;
  const distance = Math.min(Math.sqrt(deltaX ** 2 + deltaY ** 2), maxDistance);
  const angle = Math.atan2(deltaY, deltaX);

  return {
    x: Math.cos(angle) * distance,
    y: Math.sin(angle) * distance,
  };
}

function clearTimer(timer: number | undefined) {
  if (timer !== undefined) {
    window.clearTimeout(timer);
  }
}

function schedulePurpleBlink() {
  purpleBlinkTimer = window.setTimeout(() => {
    isPurpleBlinking.value = true;
    purpleBlinkOffTimer = window.setTimeout(() => {
      isPurpleBlinking.value = false;
      schedulePurpleBlink();
    }, 150);
  }, Math.random() * 4000 + 3000);
}

function scheduleBlackBlink() {
  blackBlinkTimer = window.setTimeout(() => {
    isBlackBlinking.value = true;
    blackBlinkOffTimer = window.setTimeout(() => {
      isBlackBlinking.value = false;
      scheduleBlackBlink();
    }, 150);
  }, Math.random() * 4000 + 3000);
}

function stopPeekTimers() {
  clearTimer(peekTimer);
  clearTimer(peekOffTimer);
  peekTimer = undefined;
  peekOffTimer = undefined;
}

function schedulePurplePeek() {
  stopPeekTimers();
  if (props.passwordLength <= 0 || !props.showPassword) {
    isPurplePeeking.value = false;
    return;
  }

  peekTimer = window.setTimeout(() => {
    isPurplePeeking.value = true;
    peekOffTimer = window.setTimeout(() => {
      isPurplePeeking.value = false;
      schedulePurplePeek();
    }, 800);
  }, Math.random() * 3000 + 2000);
}

watch(
  () => props.isTyping,
  (isTyping) => {
    clearTimer(lookingTimer);
    if (isTyping) {
      isLookingAtEachOther.value = true;
      lookingTimer = window.setTimeout(() => {
        isLookingAtEachOther.value = false;
      }, 800);
    } else {
      isLookingAtEachOther.value = false;
    }
  },
);

watch(
  () => [props.passwordLength, props.showPassword],
  () => schedulePurplePeek(),
);

onMounted(() => {
  window.addEventListener('mousemove', handleMouseMove);
  schedulePurpleBlink();
  scheduleBlackBlink();
  schedulePurplePeek();
});

onBeforeUnmount(() => {
  window.removeEventListener('mousemove', handleMouseMove);
  [
    purpleBlinkTimer,
    purpleBlinkOffTimer,
    blackBlinkTimer,
    blackBlinkOffTimer,
    lookingTimer,
    peekTimer,
    peekOffTimer,
  ].forEach(clearTimer);
});

const purplePos = computed(() => calculatePosition(purpleRef.value));
const blackPos = computed(() => calculatePosition(blackRef.value));
const orangePos = computed(() => calculatePosition(orangeRef.value));
const yellowPos = computed(() => calculatePosition(yellowRef.value));
const isHidingPassword = computed(() => props.passwordLength > 0 && !props.showPassword);
const isPasswordVisible = computed(() => props.passwordLength > 0 && props.showPassword);

const purpleStyle = computed(() => ({
  height: props.isTyping || isHidingPassword.value ? '440px' : '400px',
  transform: isPasswordVisible.value
    ? 'skewX(0deg)'
    : props.isTyping || isHidingPassword.value
      ? `skewX(${purplePos.value.bodySkew - 12}deg) translateX(40px)`
      : `skewX(${purplePos.value.bodySkew}deg)`,
}));

const blackStyle = computed(() => ({
  transform: isPasswordVisible.value
    ? 'skewX(0deg)'
    : isLookingAtEachOther.value
      ? `skewX(${blackPos.value.bodySkew * 1.5 + 10}deg) translateX(20px)`
      : props.isTyping || isHidingPassword.value
        ? `skewX(${blackPos.value.bodySkew * 1.5}deg)`
        : `skewX(${blackPos.value.bodySkew}deg)`,
}));

const orangeStyle = computed(() => ({
  transform: isPasswordVisible.value ? 'skewX(0deg)' : `skewX(${orangePos.value.bodySkew}deg)`,
}));

const yellowStyle = computed(() => ({
  transform: isPasswordVisible.value ? 'skewX(0deg)' : `skewX(${yellowPos.value.bodySkew}deg)`,
}));

const purpleEyesStyle = computed(() => ({
  left: isPasswordVisible.value
    ? '20px'
    : isLookingAtEachOther.value
      ? '55px'
      : `${45 + purplePos.value.faceX}px`,
  top: isPasswordVisible.value
    ? '35px'
    : isLookingAtEachOther.value
      ? '65px'
      : `${40 + purplePos.value.faceY}px`,
}));

const blackEyesStyle = computed(() => ({
  left: isPasswordVisible.value
    ? '10px'
    : isLookingAtEachOther.value
      ? '32px'
      : `${26 + blackPos.value.faceX}px`,
  top: isPasswordVisible.value
    ? '28px'
    : isLookingAtEachOther.value
      ? '12px'
      : `${32 + blackPos.value.faceY}px`,
}));

const orangeEyesStyle = computed(() => ({
  left: isPasswordVisible.value ? '50px' : `${82 + orangePos.value.faceX}px`,
  top: isPasswordVisible.value ? '85px' : `${90 + orangePos.value.faceY}px`,
}));

const yellowEyesStyle = computed(() => ({
  left: isPasswordVisible.value ? '20px' : `${52 + yellowPos.value.faceX}px`,
  top: isPasswordVisible.value ? '35px' : `${40 + yellowPos.value.faceY}px`,
}));

const yellowMouthStyle = computed(() => ({
  left: isPasswordVisible.value ? '10px' : `${40 + yellowPos.value.faceX}px`,
  top: isPasswordVisible.value ? '88px' : `${88 + yellowPos.value.faceY}px`,
}));

function pupilTransform(forceLookX?: number, forceLookY?: number) {
  if (forceLookX !== undefined && forceLookY !== undefined) {
    return `translate(${forceLookX}px, ${forceLookY}px)`;
  }
  return 'translate(0px, 0px)';
}

function trackedPupilTransform(element: HTMLElement | null, maxDistance: number) {
  const offset = calculatePupilOffset(element, maxDistance);
  return `translate(${offset.x}px, ${offset.y}px)`;
}

const purplePupilStyle = computed(() => ({
  transform: isPasswordVisible.value
    ? pupilTransform(isPurplePeeking.value ? 4 : -4, isPurplePeeking.value ? 5 : -4)
    : isLookingAtEachOther.value
      ? pupilTransform(3, 4)
      : trackedPupilTransform(purpleRef.value, 5),
}));

const blackPupilStyle = computed(() => ({
  transform: isPasswordVisible.value
    ? pupilTransform(-4, -4)
    : isLookingAtEachOther.value
      ? pupilTransform(0, -4)
      : trackedPupilTransform(blackRef.value, 4),
}));

const orangePupilStyle = computed(() => ({
  transform: isPasswordVisible.value ? pupilTransform(-5, -4) : trackedPupilTransform(orangeRef.value, 5),
}));

const yellowPupilStyle = computed(() => ({
  transform: isPasswordVisible.value ? pupilTransform(-5, -4) : trackedPupilTransform(yellowRef.value, 5),
}));
</script>

<style scoped>
.animated-characters {
  position: relative;
  width: 550px;
  height: 400px;
}

.character {
  position: absolute;
  bottom: 0;
  transform-origin: bottom center;
  transition: all 700ms ease-in-out;
}

.character-purple {
  left: 70px;
  z-index: 1;
  width: 180px;
  height: 400px;
  border-radius: 10px 10px 0 0;
  background: #6c3ff5;
}

.character-black {
  left: 240px;
  z-index: 2;
  width: 120px;
  height: 310px;
  border-radius: 8px 8px 0 0;
  background: #2d2d2d;
}

.character-orange {
  left: 0;
  z-index: 3;
  width: 240px;
  height: 200px;
  border-radius: 120px 120px 0 0;
  background: #ff9b6b;
}

.character-yellow {
  left: 310px;
  z-index: 4;
  width: 140px;
  height: 230px;
  border-radius: 70px 70px 0 0;
  background: #e8d754;
}

.character-eyes {
  position: absolute;
  display: flex;
  transition: all 700ms ease-in-out;
}

.purple-eyes {
  gap: 32px;
}

.black-eyes {
  gap: 24px;
}

.orange-eyes {
  gap: 32px;
  transition-duration: 200ms;
  transition-timing-function: ease-out;
}

.yellow-eyes {
  gap: 24px;
  transition-duration: 200ms;
  transition-timing-function: ease-out;
}

.eyeball {
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 999px;
  background: #fff;
  transition: all 150ms ease;
}

.purple-eye {
  width: 18px;
  height: 18px;
}

.black-eye {
  width: 16px;
  height: 16px;
}

.eyeball.blinking {
  height: 2px;
}

.eyeball.blinking .eyeball-pupil {
  opacity: 0;
}

.eyeball-pupil,
.dot-eye {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: #2d2d2d;
  transition: transform 100ms ease-out;
}

.black-pupil {
  width: 6px;
  height: 6px;
}

.dot-eye {
  width: 12px;
  height: 12px;
}

.yellow-mouth {
  position: absolute;
  width: 80px;
  height: 4px;
  border-radius: 999px;
  background: #2d2d2d;
  transition: all 200ms ease-out;
}

@media (max-width: 1180px) {
  .animated-characters {
    transform: scale(0.82);
    transform-origin: bottom center;
  }
}
</style>
