// in edu.usc.csci571.artsyapp.model/GenesResponse.kt

package edu.usc.csci571.artsyapp.model

import com.google.gson.annotations.SerializedName

data class GenesResponse(
    @SerializedName("_embedded") val embedded: GeneEmbedded
)

data class GeneEmbedded(
    val genes: List<GeneApi>
)

data class GeneApi(
    val id: String,
    val name: String?,
    @SerializedName("display_name") val displayName: String?,
    val description: String,
    @SerializedName("_links") val links: GeneLinks
)

data class GeneLinks(
    val thumbnail: Link
)

data class Link(
    val href: String
)
