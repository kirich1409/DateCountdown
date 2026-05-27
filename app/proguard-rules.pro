# App-specific R8 rules for DateCountdown.
#
# Philosophy: keep this file minimal. Most libraries ship consumer rules
# (kotlinx-serialization, Room, Decompose, MVIKotlin, DataStore, Metro).
# Only add a rule here when assembleRelease demonstrates a concrete need.
#
# Current state: no app-specific rules are required.
# kotlinx-serialization consumer rules cover RootComponent.Config / EditConfig
# serializer retention via the @Serializable wildcard keeps in the library jar.
