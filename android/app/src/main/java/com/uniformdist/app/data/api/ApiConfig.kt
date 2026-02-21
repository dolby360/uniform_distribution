package com.uniformdist.app.data.api

object ApiConfig {
    // Each Gen2 Cloud Function has its own Cloud Run URL
    // Replace these with actual deployed URLs from:
    //   gcloud functions describe <name> --region=us-central1 --gen2 --format="value(serviceConfig.uri)"
    const val PROCESS_OUTFIT_URL = "https://process-outfit-u7f42vzzaq-uc.a.run.app"
    const val CONFIRM_MATCH_URL = "https://confirm-match-u7f42vzzaq-uc.a.run.app"
    const val ADD_NEW_ITEM_URL = "https://add-new-item-u7f42vzzaq-uc.a.run.app"
    const val STATISTICS_URL = "https://statistics-u7f42vzzaq-uc.a.run.app"
    const val GET_ITEM_IMAGES_URL = "https://get-item-images-u7f42vzzaq-uc.a.run.app"
    const val DELETE_ITEM_IMAGE_URL = "https://delete-item-image-u7f42vzzaq-uc.a.run.app"

    // Retrofit needs a base URL even though we override per-request
    // Using a placeholder that gets overridden by the interceptor
    const val BASE_URL = "https://placeholder.uniform-dist.app/"

    const val TIMEOUT_SECONDS = 60L
}
