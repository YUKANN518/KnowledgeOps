<script setup lang="ts">
import { computed } from 'vue'
const props = defineProps<{ value: string }>()
const tone = computed(() => ({ PUBLISHED:'success', ACTIVE:'success', RESOLVED:'success', CLOSED:'neutral', ARCHIVED:'neutral', DRAFT:'warning', OPEN:'info', IN_PROGRESS:'warning', URGENT:'danger', HIGH:'danger', MEDIUM:'warning', LOW:'neutral' }[props.value] || 'neutral'))
</script>
<template><span class="status-pill" :class="`is-${tone}`"><span></span>{{ value.replaceAll('_', ' ') }}</span></template>
