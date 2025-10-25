const {getDefaultConfig, mergeConfig} = require('@react-native/metro-config');

const config = {
  resolver: {
    alias: {
      '@components': './src/components',
      '@screens': './src/screens',
      '@services': './src/services',
      '@utils': './src/utils',
      '@types': './src/types',
      '@hooks': './src/hooks',
      '@context': './src/context',
    },
  },
  transformer: {
    assetPlugins: ['expo-asset/tools/hashAssetFiles'],
  },
};

module.exports = mergeConfig(getDefaultConfig(__dirname), config);