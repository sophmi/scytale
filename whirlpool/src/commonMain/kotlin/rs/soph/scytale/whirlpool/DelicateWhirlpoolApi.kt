package rs.soph.scytale.whirlpool

@MustBeDocumented
@RequiresOptIn(
	message = "This API does not clear the internal cipher state. " +
		"Ensure you fully understand the documentation and implications.",
)
@Retention(AnnotationRetention.BINARY)
public annotation class DelicateWhirlpoolApi
