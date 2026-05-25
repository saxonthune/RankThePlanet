package com.saxonthune.ranktheplanet.domain

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

object BuiltInTemplates {
    val coffeeRanking: ReviewTemplate = ReviewTemplate(
        collectionId = CollectionId("__builtin_coffee_ranking__"),
        version = 1,
        fields = persistentListOf(
            TemplateField(
                name = "score",
                label = "Score",
                type = FieldType.Score,
                config = TemplateFieldConfig.Score(min = 0.0, max = 5.0, step = 0.5, render = "stars"),
                ordinal = 0,
            ),
            TemplateField(
                name = "roast",
                label = "Roast",
                type = FieldType.Enum,
                config = TemplateFieldConfig.Enum(
                    options = persistentListOf("light", "medium", "dark"),
                ),
                ordinal = 1,
            ),
            TemplateField(
                name = "notes",
                label = "Notes",
                type = FieldType.Text,
                config = TemplateFieldConfig.TextField,
                ordinal = 2,
            ),
        ),
    )

    val wishlist: ReviewTemplate = ReviewTemplate(
        collectionId = CollectionId("__builtin_wishlist__"),
        version = 1,
        fields = persistentListOf(
            TemplateField(
                name = "visited",
                label = "Visited",
                type = FieldType.Boolean,
                config = TemplateFieldConfig.BooleanField,
                ordinal = 0,
            ),
            TemplateField(
                name = "notes",
                label = "Notes",
                type = FieldType.Text,
                config = TemplateFieldConfig.TextField,
                ordinal = 1,
            ),
        ),
    )

    val geoDiary: ReviewTemplate = ReviewTemplate(
        collectionId = CollectionId("__builtin_geo_diary__"),
        version = 1,
        fields = persistentListOf(
            TemplateField(
                name = "entry",
                label = "Entry",
                type = FieldType.Text,
                config = TemplateFieldConfig.TextField,
                ordinal = 0,
            ),
        ),
    )

    val all: ImmutableList<ReviewTemplate> = persistentListOf(coffeeRanking, wishlist, geoDiary)
}
