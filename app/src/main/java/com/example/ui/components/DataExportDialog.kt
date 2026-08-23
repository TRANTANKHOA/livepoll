package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PollWithDetails
import com.example.ui.theme.PrimaryLight
import com.example.ui.theme.SecondaryLight
import com.example.ui.theme.TertiaryLight
import com.example.util.DataExportHelper

@Composable
fun DataExportDialog(
    pollDetails: PollWithDetails,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val poll = pollDetails.poll

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Secure Data Export",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Export and backup poll questionnaire responses, timestamps, RSVP rosters, and voting analytics in standard formats:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // CSV Export Option
                ExportFormatOption(
                    title = "CSV Spreadsheet (Excel / Google Sheets)",
                    subtitle = "Complete tabular data: options, percentages, voter names & timestamps",
                    icon = Icons.Default.TableChart,
                    color = TertiaryLight,
                    onClick = {
                        val csvData = DataExportHelper.generateCsv(pollDetails)
                        DataExportHelper.shareText(
                            context = context,
                            text = csvData,
                            title = "${poll.title} - Poll Results.csv"
                        )
                        onDismiss()
                    },
                    testTag = "export_csv_option_button"
                )

                // Summary Report Option
                ExportFormatOption(
                    title = "Formatted Text Summary Report",
                    subtitle = "Clean Markdown breakdown ready for meeting notes or group chats",
                    icon = Icons.Default.Description,
                    color = PrimaryLight,
                    onClick = {
                        val summary = DataExportHelper.generateShareableSummary(pollDetails)
                        DataExportHelper.shareText(
                            context = context,
                            text = summary,
                            title = "${poll.title} - Summary Report"
                        )
                        onDismiss()
                    },
                    testTag = "export_summary_option_button"
                )

                // JSON Backup Option
                ExportFormatOption(
                    title = "JSON Structured Data Backup",
                    subtitle = "Machine-readable raw archive with options & vote counts",
                    icon = Icons.Default.FileDownload,
                    color = SecondaryLight,
                    onClick = {
                        val jsonData = DataExportHelper.generateJsonBackup(pollDetails)
                        DataExportHelper.shareText(
                            context = context,
                            text = jsonData,
                            title = "${poll.title} - Backup.json"
                        )
                        onDismiss()
                    },
                    testTag = "export_json_option_button"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun ExportFormatOption(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
