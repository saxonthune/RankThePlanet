package com.saxonthune.ranktheplanet.domain.io

import com.saxonthune.ranktheplanet.domain.Collection
import com.saxonthune.ranktheplanet.domain.Location
import com.saxonthune.ranktheplanet.domain.ReviewInstance
import com.saxonthune.ranktheplanet.domain.ReviewTemplate

/** Decoded ids are placeholders (`CollectionId("")`, `LocationId("")`); the repo replaces them on ingest. */
data class PortableCollection(
    val collection: Collection,
    val template: ReviewTemplate?,
    val entries: List<PortableEntry>,
)

data class PortableEntry(
    val location: Location,
    val review: ReviewInstance?,
)
