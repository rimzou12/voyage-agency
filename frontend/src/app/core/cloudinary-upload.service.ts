import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../environments/environment';

interface CloudinaryUploadResponse {
  secure_url: string;
}

/**
 * Uploads a file straight from the browser to Cloudinary's unsigned upload endpoint -
 * the backend never sees the file, so it stays within Render's request size/bandwidth
 * limits. Disabled (see {@link enabled}) until a cloud name and upload preset are
 * configured in the environment files.
 */
@Injectable({ providedIn: 'root' })
export class CloudinaryUploadService {
  private readonly http = inject(HttpClient);

  readonly enabled = Boolean(environment.cloudinaryCloudName && environment.cloudinaryUploadPreset);

  /** Resolves to the uploaded image's public URL. */
  upload(file: File): Observable<string> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('upload_preset', environment.cloudinaryUploadPreset);

    const url = `https://api.cloudinary.com/v1_1/${environment.cloudinaryCloudName}/image/upload`;
    return this.http.post<CloudinaryUploadResponse>(url, formData).pipe(map((response) => response.secure_url));
  }
}
