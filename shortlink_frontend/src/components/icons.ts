import { defineComponent, h } from 'vue';

function makeIcon(name: string, glyph: string) {
  return defineComponent({
    name,
    inheritAttrs: false,
    props: {
      size: {
        type: Number,
        default: 18
      }
    },
    setup(props, { attrs }) {
      return () =>
        h(
          'span',
          {
            ...attrs,
            class: ['icon-glyph', attrs.class],
            style: [
              {
                width: `${props.size}px`,
                height: `${props.size}px`,
                fontSize: `${Math.max(12, props.size * 0.78)}px`
              },
              attrs.style as Record<string, string> | undefined
            ],
            'aria-hidden': 'true'
          },
          glyph
        );
    }
  });
}

export const ArrowDown = makeIcon('ArrowDown', 'v');
export const ArrowUp = makeIcon('ArrowUp', '^');
export const BarChart3 = makeIcon('BarChart3', '▥');
export const Check = makeIcon('Check', '✓');
export const ChevronLeft = makeIcon('ChevronLeft', '‹');
export const ChevronRight = makeIcon('ChevronRight', '›');
export const Clock = makeIcon('Clock', '◷');
export const Copy = makeIcon('Copy', '⧉');
export const ExternalLink = makeIcon('ExternalLink', '↗');
export const Globe = makeIcon('Globe', '◎');
export const Inbox = makeIcon('Inbox', '□');
export const KeyRound = makeIcon('KeyRound', '⌘');
export const Layers = makeIcon('Layers', '▤');
export const Link2 = makeIcon('Link2', '⌁');
export const LoaderCircle = makeIcon('LoaderCircle', '○');
export const LogOut = makeIcon('LogOut', '⇥');
export const Mail = makeIcon('Mail', '@');
export const MousePointerClick = makeIcon('MousePointerClick', '◉');
export const Pencil = makeIcon('Pencil', '✎');
export const Phone = makeIcon('Phone', '☎');
export const Plus = makeIcon('Plus', '+');
export const RefreshCw = makeIcon('RefreshCw', '↻');
export const Save = makeIcon('Save', '▣');
export const Search = makeIcon('Search', '⌕');
export const Sparkles = makeIcon('Sparkles', '✦');
export const Trash2 = makeIcon('Trash2', '×');
export const UserRound = makeIcon('UserRound', '●');
export const X = makeIcon('X', '×');
