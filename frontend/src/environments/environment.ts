/** Local development default - swapped for environment.prod.ts in production builds (see angular.json). */
export const environment = {
  apiBaseUrl: 'http://localhost:8080',
  // Cloudinary unsigned upload target for hotel photos. Create a free account at
  // cloudinary.com, then Settings -> Upload -> Upload presets -> Add upload preset
  // with Signing Mode "Unsigned", and fill in both values below (and in
  // environment.prod.ts). Leave blank to disable the upload button (falls back to
  // pasting URLs by hand).
  cloudinaryCloudName: 'm7pnmofu',
  cloudinaryUploadPreset: 'photosu',
};
