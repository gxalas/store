package com.example.pdfreader.Controllers.States;

import com.example.pdfreader.Entities.Main.Document;
import javafx.scene.control.TableColumn;

public record PreviewFileViewState(int type, TableColumn tableColumn,Document selected) {
}
