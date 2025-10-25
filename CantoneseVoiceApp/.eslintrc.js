module.exports = {
  root: true,
  extends: [
    '@react-native-community',
    'plugin:@typescript-eslint/recommended',
  ],
  parser: '@typescript-eslint/parser',
  plugins: ['@typescript-eslint'],
  rules: {
    // 自定义规则
    '@typescript-eslint/no-unused-vars': 'error',
    'react-native/no-inline-styles': 'warn',
  },
};