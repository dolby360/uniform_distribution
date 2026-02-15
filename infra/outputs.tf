output "service_account_email" {
  value = google_service_account.main.email
}

output "storage_bucket_name" {
  value = google_storage_bucket.app.name
}

output "storage_bucket_url" {
  value = google_storage_bucket.app.url
}

output "firestore_database" {
  value = google_firestore_database.default.name
}
