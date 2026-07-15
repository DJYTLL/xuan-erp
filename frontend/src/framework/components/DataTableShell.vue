<template>
  <section class="table-shell">
    <header v-if="$slots.toolbar" class="table-shell-toolbar">
      <slot name="toolbar" />
    </header>
    <div class="table-shell-body">
      <slot />
    </div>
    <footer v-if="$slots.pagination" class="table-shell-pagination">
      <slot name="pagination" />
    </footer>
  </section>
</template>
