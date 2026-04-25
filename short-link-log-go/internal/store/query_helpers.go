package store

// appendLogServiceBucket appends a service equality filter to the WHERE clause.
// If service is empty, the filter is skipped so all services are returned.
func appendLogServiceBucket(where *[]string, args *[]any, service string) {
	if service == "" {
		return
	}
	*where = append(*where, "service = ?")
	*args = append(*args, service)
}
