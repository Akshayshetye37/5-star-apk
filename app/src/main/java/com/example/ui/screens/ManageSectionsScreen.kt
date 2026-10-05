package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppSection
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.HotelNavy
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.HotelViewModel

@Composable
fun ManageSectionsScreen(
    viewModel: HotelViewModel
) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()

    var editingSection by remember { mutableStateOf<AppSection?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingSection = null
                    showAddDialog = true
                },
                containerColor = HotelNavy,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Section")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("manage_sections_screen")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Customize Navigation Tabs & Sections", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HotelNavy)
                Text(
                    "Enable, disable, rename, or reorder app sections. Disabling only hides the menu entry without deleting any data.",
                    fontSize = 12.sp,
                    color = Slate500
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(sections, key = { _, s -> s.id }) { index, section ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = section.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (section.isEnabled) Slate800 else Slate500
                                    )
                                    if (section.isCore) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(Icons.Default.Lock, contentDescription = "Core Section", modifier = Modifier.size(14.dp), tint = Slate500)
                                    }
                                }
                                Text("ID: ${section.id}", fontSize = 11.sp, color = Slate500)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Up / Down reorder
                                IconButton(
                                    onClick = {
                                        if (index > 0) {
                                            val prev = sections[index - 1]
                                            viewModel.updateSection(section.copy(orderIndex = index - 1))
                                            viewModel.updateSection(prev.copy(orderIndex = index))
                                        }
                                    },
                                    enabled = index > 0
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(18.dp))
                                }

                                IconButton(
                                    onClick = {
                                        if (index < sections.size - 1) {
                                            val next = sections[index + 1]
                                            viewModel.updateSection(section.copy(orderIndex = index + 1))
                                            viewModel.updateSection(next.copy(orderIndex = index))
                                        }
                                    },
                                    enabled = index < sections.size - 1
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(18.dp))
                                }

                                // Edit Rename
                                IconButton(onClick = { editingSection = section }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Rename", modifier = Modifier.size(18.dp), tint = Slate700)
                                }

                                // Enable/Disable Toggle
                                Switch(
                                    checked = section.isEnabled,
                                    onCheckedChange = { viewModel.toggleSection(section) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Rename / Edit Dialog
    editingSection?.let { sec ->
        var newTitle by remember { mutableStateOf(sec.title) }

        AlertDialog(
            onDismissRequest = { editingSection = null },
            title = { Text("Rename Section", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Section Label") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            viewModel.updateSection(sec.copy(title = newTitle.trim()))
                            editingSection = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { editingSection = null }) { Text("Cancel") }
            }
        )
    }

    // Add Section Dialog
    if (showAddDialog) {
        var sectionTitle by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Custom Section", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = sectionTitle,
                        onValueChange = { sectionTitle = it },
                        label = { Text("Section Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (sectionTitle.isNotBlank()) {
                            val newSec = AppSection(
                                id = sectionTitle.trim().uppercase().replace(" ", "_"),
                                title = sectionTitle.trim(),
                                iconName = "Custom",
                                orderIndex = sections.size,
                                isEnabled = true,
                                isCore = false
                            )
                            viewModel.updateSection(newSec)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
                ) {
                    Text("Add Section")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}
