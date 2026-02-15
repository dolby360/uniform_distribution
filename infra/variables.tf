variable "project_id" {
  description = "GCP project ID"
  type        = string
}

variable "project_number" {
  description = "GCP project number"
  type        = string
}

variable "region" {
  description = "GCP region"
  type        = string
  default     = "us-central1"
}

variable "service_account_name" {
  description = "Service account display name"
  type        = string
  default     = "uniform-distribution"
}

variable "gcloud_access_token" {
  description = "Access token from gcloud auth print-access-token"
  type        = string
  sensitive   = true
}
