package store

// appendLogServiceBucket appends a service-name equality filter to the WHERE
// clause and args slice used when building ClickHouse queries.
// If service is empty the filter is skipped so that all services are returned.
func appendLogServiceBucket(where *[]string, args *[]any, service string) {
	if service == "" {
		return
	}
	*where = append(*where, "service = ?")
	*args = append(*args, service)
}
