# Craft & Find — карта модульных GUI-текстур

Все активные элементы находятся в namespace `craftandfind`. Каждый PNG можно перерисовать отдельно без изменений Java-кода.

## Основной экран

- `workbench_panel.png` — `176×166`, только фон и внешняя рамка.
- `crafting_slot.png` — `18×18`, один слот сетки 3×3.
- `inventory_slot.png` — `18×18`, один слот инвентаря/хотбара.
- `result_slot.png` — `26×26`, рамка результата крафта.
- `crafting_arrow.png` — `22×15`, стрелка результата.
- `storage_workbench.png` — собранный эталонный preview старого вида; код его больше не использует.

## Складская вкладка

- `storage_panel.png` — `147×166`, фон панели.
- `search_box.png` — `87×18`, неактивное поле.
- `search_box_focused.png` — `87×18`, активное поле.
- `sort_button.png` — `20×18`, обычное состояние.
- `sort_button_hovered.png` — `20×18`, наведение.
- `sort_button_selected.png` — `20×18`, сортировка по количеству.
- `storage_slot.png` — `25×25`, ячейка предмета.
- `storage_slot_hovered.png` — `25×25`, ячейка под курсором.
- `scrollbar_track.png` — `6×125`, основная полоса.
- `scrollbar_thumb.png` — `6×18`, ползунок.
- `scrollbar_thumb_hovered.png` — `6×18`, активный/перетаскиваемый ползунок.

## Боковые кнопки

- `tab_button.png` — `20×18`, обычная кнопка.
- `tab_button_hovered.png` — `20×18`, наведение.
- `tab_button_selected.png` — `20×18`, открытая вкладка.
- `icons/compass.png` — `16×16`.
- `icons/recipe_book.png` — `16×16`.
- `icons/search.png` — `16×16`.

## Книга рецептов

- `recipe_panel.png` — атлас `256×256`; рабочая область панели остаётся совместимой с ванильной логикой книги.
- `textures/gui/sprites/workbench/recipe_slot_available.png` — доступный рецепт.
- `textures/gui/sprites/workbench/recipe_slot_unavailable.png` — недоступный рецепт.
- `textures/gui/sprites/workbench/recipe_slot_many_available.png` — группа доступных рецептов.
- `textures/gui/sprites/workbench/recipe_slot_many_unavailable.png` — группа недоступных рецептов.
- `textures/gui/sprites/workbench/category_tab.png` — вкладка категории.
- `textures/gui/sprites/workbench/category_tab_selected.png` — выбранная вкладка.
- `textures/gui/sprites/workbench/page_next*.png` — следующая страница.
- `textures/gui/sprites/workbench/page_previous*.png` — предыдущая страница.

Файлы в `textures/gui/sprites` являются GUI-atlas sprites и используются ванильными виджетами книги только внутри `StorageWorkbenchScreen`.

## Где менять размеры и координаты

```text
client/gui/workbench/WorkbenchLayout.java
```

## Где менять пути к изображениям

```text
client/gui/workbench/WorkbenchTextures.java
```
