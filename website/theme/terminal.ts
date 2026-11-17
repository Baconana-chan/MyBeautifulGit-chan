/**
 * Shiki theme built from the app's DarkPalette (shared/src/ui/mockup/Theme.kt).
 * Nothing here is red except genuinely invalid syntax, so code blocks do not
 * fight the green terminal look of the rest of the site.
 */
export const terminalTheme = {
  name: 'git-chan-terminal',
  displayName: 'Git-chan terminal',
  type: 'dark',
  colors: {
    'editor.background': '#0D120C',
    'editor.foreground': '#C7D9C1',
  },
  settings: [
    {
      scope: ['comment', 'punctuation.definition.comment'],
      settings: { foreground: '#758B6F', fontStyle: 'italic' },
    },
    {
      scope: ['keyword', 'keyword.control', 'storage', 'storage.type', 'storage.modifier'],
      settings: { foreground: '#8CFF9B' },
    },
    {
      scope: ['string', 'string.quoted', 'string.template', 'punctuation.definition.string'],
      settings: { foreground: '#FFC857' },
    },
    {
      scope: ['constant.numeric', 'constant.language', 'constant.character'],
      settings: { foreground: '#FFC857' },
    },
    {
      scope: ['entity.name.function', 'support.function', 'support.function.builtin'],
      settings: { foreground: '#6CD3FF' },
    },
    {
      scope: ['variable', 'variable.parameter', 'variable.other'],
      settings: { foreground: '#C7D9C1' },
    },
    {
      scope: ['keyword.operator', 'keyword.operator.redirect', 'punctuation.definition.redirect'],
      settings: { foreground: '#57A868' },
    },
    {
      scope: ['punctuation', 'punctuation.separator', 'meta.punctuation'],
      settings: { foreground: '#7E9678' },
    },
    {
      scope: ['entity.name.tag', 'support.class.component'],
      settings: { foreground: '#8CFF9B' },
    },
    {
      scope: ['entity.other.attribute-name'],
      settings: { foreground: '#6CD3FF' },
    },
    {
      scope: ['invalid', 'invalid.illegal'],
      settings: { foreground: '#FF6B5E' },
    },
  ],
}
