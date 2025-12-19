variable "region" {
  description = "AWS Region"
  default     = "ap-northeast-2"
}

variable "project_name" {
  description = "Project name"
  type        = string
  default     = "TT"
}

# 리소스 태그
variable "team_tag" {
  description = "Team tag value"
  type        = string
  default     = "devcos-teamb2"
}

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t3.small"
}

variable "root_volume_size" {
  description = "Root volume size in GB"
  type        = number
  default     = 20
}