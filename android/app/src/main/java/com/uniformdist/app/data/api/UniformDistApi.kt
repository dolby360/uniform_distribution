package com.uniformdist.app.data.api

import com.uniformdist.app.data.model.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

interface UniformDistApi {

    @POST
    suspend fun processOutfit(
        @Url url: String = ApiConfig.PROCESS_OUTFIT_URL,
        @Body request: ProcessOutfitRequest
    ): ProcessOutfitResponse

    @POST
    suspend fun confirmMatch(
        @Url url: String = ApiConfig.CONFIRM_MATCH_URL,
        @Body request: ConfirmMatchRequest
    ): ConfirmMatchResponse

    @POST
    suspend fun addNewItem(
        @Url url: String = ApiConfig.ADD_NEW_ITEM_URL,
        @Body request: AddNewItemRequest
    ): AddNewItemResponse

    @GET
    suspend fun getStatistics(
        @Url url: String = ApiConfig.STATISTICS_URL
    ): StatisticsResponse
}
