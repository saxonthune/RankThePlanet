// GENERATED FILE — do not edit by hand.
// Source: .carta/02-design/02-interaction/01-navigation.statechart.json
// Regenerate with: cd tools/statechart && npm run build
//
// This file exists so the Stately VS Code extension can statically follow the
// createMachine literal. The carta sidecar JSON is the source of truth.

import { createMachine } from 'xstate';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export const navigationMachine = createMachine({
  /** @xstate-layout N4IgpgJg5mDOIC5QCcAuAHAtAOwIYDcBLKXVQge2wDo98qBZXdAeXzGSLAHcBiAFQCCABQD6QgJIA5ANoAGALqJQ6crEJlKSkAA9EAVgDsAZioBOA6YBMsgIx7LN43psAaEAE9ENozdlU9ACyWplamskYB4XoAvtFuaFi0xKQU1LQMTKzsnLyCogDCzAAyRQCi+XzizJIifMwA4vVlcopIICpqGthaugjeelRGQxF6ABwBNsHGbp59wX4GNgBsRpaj9lNLsfEYOATJXTQEGSxsHITc-MIiAFIAqvSidSKFJeWV1S1aHeqpPV5GAbDEbjSbmIwzLw2AIBQajWSGJaOUaOSxGbYgBJ7IgkQ7pRinbIXXLXV5lCpVSQAZREACE7nw6jIFN9VL9NG1enpZLDZKMhkiHMZRijIQhjKYqJYgqMlnzFmNHBisUlcakjnQCVlzpc8iIBAARA0iAASpQASqUvm0fl1-ggfAETA5fI4DPMDAYxdDTEsqAi1ktRtLTBNJsrdqqUpQNSdtTkrqIqaVGVJ6lTrco2XbOYgAuN-fyjILHEYRa4PPpDFQ7EtDFZlls4pjI-s1TH0vlyAAbbtgADGXSKhFgqETL2K5I+zNaWc6f1zfWLfqMBm5gNkS0WSK9lfF+aocoRfO5TvhBgjiTb0bSxy7vYHQ5HY71klKAHUJ28KZ8WTbswuoC9I6zo2K6noerusxypYVAGMGeg+FMKKApe2IHOqnY9n2g6pMOo7juIjzMOafCZu0AEckBiAgVKYG2BBYSemKAR6H6MJbvBpiIbIsimKMaFRnid7YY+eHPjwtICPkADS5G2oBOh5qGhYCpMpblmKdYGGYrHrKx9iBN4gnXsJdD3jhXQGmAqC4IQ3bjqUkh8OaACa8mUd0i7QpuZhyqG0pyksljut6aIrhMej8Ty3HwpYJk4jesYWWJlDWbZ9njoaxpOS57l-nO7JedRDrQqB4HukxUFQtKNamGBESjPxTgxM2KqmZhIkPrhaU2XZDl6qUBriHwtSlI8RQCHwVoFRR85UUppVOnRFWQWK5g6esiEIYhpg+KYCUYR2XWWak6X9Y5AAaQgkWRs0KQtvRohYgzcfVRi8YYQzVeKwZUKxPKyAYAQGLIDh6K1OxXolZlUClPXYOdmV6lSJoCJaHnzcVi3PZKRhvd4n3GKuWnFjW0J1s4lhGUqbWtjDnXmaJCNIw5UmyZjRX2s4vJFiWwqinuoxrq93hLJEPi+PFdPQ0dt5M91Vl9fZVAALZMEIyDkAAVql2DjnUjRlGI5rMDc7yUpzOYlZY0qbVFEz1QEW7CxCe5gVucG8QDtshLbASHe28tw8zSsZd2VDds+ms63rBsNE0pQm2bFu-rOc1c4u-v26GNhOy70zu3y7HhDC+O+OWAky+hQexqU2CoMg7is45w2jZaABq4gflbinAWVK0MZVQM-WLkrWCK8H2MsPgXtXQmM1Q9eN83ysDdczBCE5IilJd03mpIAhFL3j1eJusHBeu4zO8FljenYm1rki3GAkEYyB0l6TL03Ld6paABilpUYiCKMwfIU1Lb3U8vaXwwVDzU3CNfJYt9vS2BsP4fGspFhhFvtLKGNdP7HG-qvcO45LT0GYB3JOuU3In2xr0HyfpfR8SCM7c+oUi58i9iKfGZYvpNnwQvY6dBiEt3ZnJKBWMYGRCYf5VhQUQp33dnYAYPJWL1WMAifGc9BEdWEVQKkNkyDYCgLASS0kJHpwevQvMwNVLFnUgLCsswQYDHWNxd0TU1xRUhi2WWtdyBnG7LgdwVAijkH7DeIQhB+wAGt2D8FclvEQABFO4Fp8pWOgYuZcfpZQhBPI4GESjZjQjzl7D6vELBNWWDovxBDDiBPYME0J4TIldGiXEhJBpTaiAkDOVkUiclBj9N7MGdhVh7RKV4YIsF3FoKBg1NcH9GlBJCWEiJUSYnxOQDwIQAgqTTREHcc0RQRAkREP-cQzRJGZxKrkqg6wgzGEiNxaEo8Kb+DRNTYWlgVhDCrrohmMYmnIBaRs9pqROk7L2eIWSIggF3CKHdLJQySqRFBv4MGw8mrcWpt6QwsJxgjPdIEViNgVnqlBeCtpWyum7PyKA5MdD7QYr8NyRRYRcWGW9CDUY-pgiTAMMgus-FKUgrWaEgQEAIB8HIPDLosL4VklTgM-8aLFqRHhFizlfIQg8r3A4NYh5+JWARCo4m4rqDUvWdK2V8rQ6pHHG+T8KqfxqsKtbTVQN2XYvmNy-FhreLoP0ntIGwsLBlitVQG1UqZVyoVU6xlzBmW3K9b0LVvrdUBr0GKW2EN-BBHqmDJ0gIQrRtjVQc0YAcj-3IMgVWVBIC-BMTwIaI1LndyKAaFli5WIg38NCNhHL1jeisLCEGNgmoBD2s4X0FbJVVprcSOtDam0QBbVAHgjLSjo07aUbtvb0Uzr8CiewZZQh8UBN6T0-KxbQmpmEfGtsF3NPWdW2t9bG3NuMVulGdxaT0BGkezVso-RCuBsLIMaIAh5q0f4Wdip+RktfWC99y7uCru-Ru3945wGSHyAekDXIjz+giJuKwQxFljqBiapq1hnAQ2Bqh8FH6V1fqoLAAArgAI1VuoXDVIANAcZENYj+hSOlwo2iD6iw4MDsmCKUMrtBQCPqUI61i6qT9gABZgHVrSLj9kIDdNKFct8Y0JpTRmqiu5oHIh+RWB7cl+YxTrB0sg6d+M2JOnqix9Z2m9MGaM92Ezuz22jWmlZ6a4mED5mWP6GETpqarDdGKRY6Dhh52WB7QEan2rAs02+0JgX9O4EM8ZhJdxkx0juNc0aUhYvxdGUl1YiEhQ-R82Ycwvm-lBh8Pl+mcsY1ad02ViroXunVBs4MuzGaApwVDJEYI2i-nrQHWuJ9YMxihisP5krY3guVYZUymb6q5vKSCItmEYM9rmDW0XA8gR+QIgdk1BE+2qDiFVioNA-9uzkF4AaabTWfU6qWPVQwIpuSjBYlFLFKIHDeeLCDT733fuoH+4D7dp3QeYo5RDh+0O+RilXPyl0YFuTCtWEEWIzZsDkBM-ANoBW5azfTYgTAecxSYGWjO22EPbCC5PNG2g7O+6IGCKT2qLymMonccKgO889HBy1GcHI4vT7ilHsU-09g+TF09NU0XJ09b4VQJrmxCB9JwRecgssAYfDpdo+EWwwZfTCz4gdZXhXkqOt6uHS3MDPTj1l77cwW5c17kh3BWUdh+J2FsHtn3w2sKKzOmvNWGsta6wRkH7ywYdIhRGOH4Va5UF7XJhG7iMJ+TcRNwrU6Af+qR2jjnvW+ebZjD9GiKdyxrAvOmX0PVNZwa2yR2iedKfa5fwbj-NenfFphElAn29MIQi529KsfloMUd6sWB9dE0-CF0EMagX9zPPUS+t+LV6YxFgIjrIGMUiFYTC8BMGIUsp9uL+AjxL5ZaT8wYjgSw3onsnocokQfIgIFceC6mKuI2xWNAlAYAv+iAR4fgEQ2kTEtsYEvKyCZgwaK2oYl8R+QKw2latKHS2y7AaBDoUOAB9gQBQooB7sEM6CgQEOYQ9gyCvocBrOASi6dqCa-u2M1i9ogI8EjBIUbEwBwqeatsJgiwwsecDgSIqwn2bGmGX6dBSWcydgaI9Gy+167sQUh46wDi8IDYyyx+qySBWhXAWG66m6uhbERe8IfEtS+urBswDGK4dgIoN8UwiEmhGGjhHG3GfGAmhAJidBVOJgCI+YnEDUJMQacogwXBD8SIQYYqthVKo2QW5WIWYWuhAMjyBhF68ws660TUg6K2xY4wDg6waOP29amOAOXAdBkh-K56TBshLBLEaIdEQw-EdYKwMoTYsQQAA */
  "id": "rtp-navigation",
  "type": "parallel",
  "description": "Top-level parallel machine. The user's experience at any moment is the product of the active `nav` state (which primary screen they are on) and the active `overlay` state (which sheet/modal is on top, if any). Events are defined on the surface where the user invokes them; cross-region targets use absolute paths.",
  "states": {
    "nav": {
      "description": "The primary screen the user is on. Persistent — does not change when overlays open and close.",
      "initial": "MapOverview",
      "states": {
        "MapOverview": {
          "description": "The everything view. Default landing surface. Shows pins for every entry in every visible Collection, colored by Collection appearance and styled by visited/unvisited state. See doc01.03 §4.",
          "tags": [
            "landing",
            "map"
          ],
          "meta": {
            "surface": "MapOverview",
            "reads": [
              "all-collections",
              "all-entries",
              "viewport"
            ]
          },
          "on": {
            "TAP_PIN": {
              "target": "EntryDetail",
              "description": "User taps a pin on the map to inspect that entry.",
              "actions": [
                "MapOverview.selectPin"
              ]
            },
            "TAP_COLLECTION_TOGGLE": {
              "description": "User toggles a Collection's visibility from the legend. Stays on this surface; the pin set updates.",
              "actions": [
                "MapOverview.toggleCollection"
              ]
            },
            "TAP_JUMP_TO_COLLECTION": {
              "description": "User picks a Collection from the legend to fit the viewport to its entries.",
              "actions": [
                "MapOverview.jumpToCollection"
              ]
            },
            "TAP_COLLECTIONS_BUTTON": {
              "target": "CollectionList",
              "description": "User opens the list of Collections to manage them."
            },
            "TAP_ADD_HERE": {
              "target": "#rtp-navigation.overlay.AddToCollection",
              "description": "User drops a pin at their current location (or a tapped map point) and chooses which Collection to add it to. Starts the Location-first flow from doc01.02 §3.",
              "actions": [
                "Location.dropPin"
              ]
            },
            "TAP_SETTINGS": {
              "target": "Settings",
              "description": "User opens settings (providers, BYOK keys, sync target)."
            }
          }
        },
        "CollectionList": {
          "description": "Manage the user's Collections. Create new ones, open existing ones, import external lists.",
          "tags": [
            "list"
          ],
          "meta": {
            "surface": "CollectionList",
            "reads": [
              "collections"
            ]
          },
          "on": {
            "TAP_COLLECTION": {
              "target": "CollectionDetail",
              "description": "User opens a Collection to see its entries."
            },
            "TAP_NEW_COLLECTION": {
              "target": "#rtp-navigation.overlay.SchemaBuilder",
              "description": "User starts a new Collection. Opens the SchemaBuilder overlay to author the Review template before any entries can be added.",
              "actions": [
                "Collection.create"
              ]
            },
            "TAP_IMPORT": {
              "target": "#rtp-navigation.overlay.ImportFlow",
              "description": "User imports an external collection (KML, GeoJSON, RTP bundle, third-party share URL).",
              "actions": [
                "Collection.import"
              ]
            },
            "BACK": {
              "target": "MapOverview",
              "description": "Return to the everything view."
            }
          }
        },
        "CollectionDetail": {
          "description": "One Collection: its entries shown as either a map or a list projection (toggleable). Entry point for adding/removing entries, editing the template, and exporting/sharing.",
          "tags": [
            "detail"
          ],
          "meta": {
            "surface": "CollectionDetail",
            "reads": [
              "collection",
              "entries",
              "appearance"
            ]
          },
          "initial": "mapProjection",
          "states": {
            "mapProjection": {
              "description": "Entries shown as pins on a map fitted to the Collection's bounds.",
              "tags": [
                "map"
              ],
              "on": {
                "TAP_TOGGLE_PROJECTION": {
                  "target": "listProjection",
                  "description": "User switches to the list view of this Collection."
                }
              }
            },
            "listProjection": {
              "description": "Entries shown as a scrollable list, sortable by date, distance, or power-ranking.",
              "tags": [
                "list"
              ],
              "on": {
                "TAP_TOGGLE_PROJECTION": {
                  "target": "mapProjection",
                  "description": "User switches to the map view of this Collection."
                }
              }
            }
          },
          "on": {
            "TAP_ENTRY": {
              "target": "EntryDetail",
              "description": "User opens a single entry to inspect its Location and Review."
            },
            "TAP_ADD_ENTRY": {
              "target": "#rtp-navigation.overlay.LocationPicker",
              "description": "User starts the Collection-first add flow. LocationPicker resolves a Location, then ReviewForm captures the per-entry Review."
            },
            "TAP_EDIT_TEMPLATE": {
              "target": "#rtp-navigation.overlay.SchemaBuilder",
              "description": "User opens the Review template editor for this Collection.",
              "actions": [
                "Review.editTemplate"
              ]
            },
            "TAP_EXPORT": {
              "description": "User exports this Collection as a portable bundle (KML / GeoJSON / RTP bundle).",
              "actions": [
                "Collection.export"
              ]
            },
            "TAP_SHARE": {
              "description": "User produces a shareable artifact. Sharing concept deferred — see doc01.04.",
              "actions": [
                "Collection.share"
              ]
            },
            "BACK": {
              "target": "MapOverview",
              "description": "Return to the everything view."
            }
          }
        },
        "EntryDetail": {
          "description": "One entry — the (Location, Review) pair. Read-mostly surface; mutation goes through the ReviewForm overlay. Hosts the 'open externally' handoff to Google Maps / Apple Maps / etc.",
          "tags": [
            "detail"
          ],
          "meta": {
            "surface": "EntryDetail",
            "reads": [
              "entry",
              "location",
              "review"
            ]
          },
          "on": {
            "TAP_EDIT_REVIEW": {
              "target": "#rtp-navigation.overlay.ReviewForm",
              "description": "User edits the Review for this entry.",
              "actions": [
                "Review.edit"
              ]
            },
            "TAP_OPEN_EXTERNAL": {
              "description": "User hands off to an external map app (Google Maps, Apple Maps, OsmAnd, ...) for navigation or street view. RTP does not track what happens after.",
              "actions": [
                "Location.openExternally"
              ]
            },
            "TAP_REFRESH_LOCATION": {
              "description": "If the Location is refreshable, re-query the provider for updated metadata.",
              "actions": [
                "Location.refresh"
              ]
            },
            "TAP_REMOVE_ENTRY": {
              "target": "CollectionDetail",
              "description": "User removes this entry from the Collection. Does not delete the Location — it may exist in other Collections.",
              "actions": [
                "Collection.removeEntry"
              ]
            },
            "BACK": {
              "target": "CollectionDetail",
              "description": "Return to the Collection."
            }
          }
        },
        "Settings": {
          "description": "Configure providers, BYOK keys, sync target. Surface is intentionally under-specified until the relevant concept actions exist.",
          "tags": [
            "modal"
          ],
          "meta": {
            "surface": "Settings",
            "reads": [
              "settings"
            ]
          },
          "on": {
            "BACK": {
              "target": "MapOverview",
              "description": "Return to the everything view."
            }
          }
        }
      }
    },
    "overlay": {
      "description": "What sheet/modal is currently overlaid on top of the nav surface. Transitions to non-`none` open an overlay; transitions back to `none` close it. Multi-target transitions can simultaneously close an overlay and change the underlying nav state (see ReviewForm.submitting.SUBMITTED).",
      "initial": "none",
      "states": {
        "none": {
          "description": "Rest state — no overlay. The user sees only the underlying nav surface.",
          "tags": [
            "rest"
          ]
        },
        "LocationPicker": {
          "description": "Resolve, drop, or import a Location. Search via a configured provider, drop a manual pin, or import from a URL / file (KML, share link).",
          "tags": [
            "picker"
          ],
          "meta": {
            "surface": "LocationPicker",
            "reads": [
              "provider-results"
            ]
          },
          "on": {
            "TYPE_QUERY": {
              "description": "User types a search query; provider returns candidates.",
              "actions": [
                "Location.resolve"
              ]
            },
            "DROP_PIN": {
              "description": "User long-presses the map to drop a manual pin.",
              "actions": [
                "Location.dropPin"
              ]
            },
            "PASTE_URL_OR_FILE": {
              "description": "User pastes a share URL or selects a KML/GeoJSON file.",
              "actions": [
                "Location.import"
              ]
            },
            "PICK_RESULT": {
              "target": "ReviewForm",
              "description": "User picks one candidate; advance to fill out the Review."
            },
            "CLOSE": {
              "target": "none",
              "description": "Cancel the add flow; close the overlay."
            }
          }
        },
        "AddToCollection": {
          "description": "Location-first flow: a Location is already chosen (dropped pin, shared link, etc.). User picks which Collection to add it to.",
          "tags": [
            "picker"
          ],
          "meta": {
            "surface": "AddToCollection",
            "reads": [
              "collections",
              "candidate-location"
            ]
          },
          "on": {
            "PICK_COLLECTION": {
              "target": "ReviewForm",
              "description": "User picks a Collection; advance to fill out the Review.",
              "actions": [
                "Location.addToCollection"
              ]
            },
            "TAP_NEW_COLLECTION": {
              "target": "SchemaBuilder",
              "description": "None of the existing Collections fit; create a new one.",
              "actions": [
                "Collection.create"
              ]
            },
            "CLOSE": {
              "target": "none",
              "description": "Cancel; close the overlay. Underlying nav remains where it was."
            }
          }
        },
        "ReviewForm": {
          "description": "Author or edit a Review instance against the Collection's template. Two substates: `editing` (the form is open) and `submitting` (commit in flight).",
          "tags": [
            "form"
          ],
          "meta": {
            "surface": "ReviewForm",
            "reads": [
              "template",
              "draft-review"
            ]
          },
          "initial": "editing",
          "states": {
            "editing": {
              "description": "User is filling in template fields.",
              "on": {
                "EDIT_FIELD": {
                  "description": "User changes the value of any field.",
                  "actions": [
                    "Review.edit"
                  ]
                },
                "CLEAR_FIELD": {
                  "description": "User explicitly unsets a field. Distinct from 'false' for booleans (three-state widget).",
                  "actions": [
                    "Review.clear"
                  ]
                },
                "TAP_SUBMIT": {
                  "target": "submitting",
                  "description": "User submits the Review.",
                  "actions": [
                    "Review.submit"
                  ]
                },
                "TAP_CANCEL": {
                  "target": "#rtp-navigation.overlay.none",
                  "description": "User cancels without saving. Draft is discarded, overlay closes."
                }
              }
            },
            "submitting": {
              "description": "Submit in flight. Brief transient state; transitions both close the overlay and route nav to EntryDetail of the just-saved entry.",
              "on": {
                "SUBMITTED": {
                  "target": "#rtp-navigation.overlay.none",
                  "description": "Submit succeeded; close the overlay. (Underlying nav routing to EntryDetail will be handled by an action once the visualizer tolerates multi-target transitions; see doc02.02.01.)"
                }
              }
            }
          }
        },
        "SchemaBuilder": {
          "description": "Author or edit a Collection's Review template — the schema that governs every Review in this Collection. Add/remove/reorder fields; pick field types (stars, enum, boolean, date, power-ranking, text). Adopt a built-in template as a starting point.",
          "tags": [
            "form"
          ],
          "meta": {
            "surface": "SchemaBuilder",
            "reads": [
              "template"
            ]
          },
          "on": {
            "DEFINE_TEMPLATE": {
              "description": "Initial template authoring at Collection creation.",
              "actions": [
                "Review.defineTemplate"
              ]
            },
            "EDIT_TEMPLATE": {
              "description": "Modify an existing template. Existing Reviews reconcile against the new shape.",
              "actions": [
                "Review.editTemplate"
              ]
            },
            "USE_BUILT_IN": {
              "description": "Adopt a built-in template (Coffee Ranking, Wishlist, Geo Diary) as the starting point.",
              "actions": [
                "Review.useBuiltIn"
              ]
            },
            "DONE": {
              "target": "none",
              "description": "Template authored or edited; close the overlay. Caller's underlying nav resumes — typically CollectionDetail."
            },
            "CLOSE": {
              "target": "none",
              "description": "Cancel without committing template changes."
            }
          }
        },
        "ImportFlow": {
          "description": "Import an external collection. Stub — concrete steps (source picker, schema mapping, preview, confirm) will be detailed when the import surface is unfolded.",
          "tags": [
            "modal"
          ],
          "meta": {
            "surface": "ImportFlow"
          },
          "on": {
            "DONE": {
              "target": "none",
              "description": "Import committed; close the overlay. Underlying nav (CollectionList) refreshes."
            },
            "CLOSE": {
              "target": "none",
              "description": "Cancel the import."
            }
          }
        }
      }
    }
  }
} as any as any);
