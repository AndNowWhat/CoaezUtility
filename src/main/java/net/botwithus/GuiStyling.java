package net.botwithus;

import net.botwithus.rs3.imgui.ImGui;

public class GuiStyling {
    private int stylesPushed = 0;
    private int colorsPushed = 0;
    private float windowRounding = 6.0f;          // More rounded corners for modern look
    private float frameRounding = 4.0f;           // Subtle rounding on frames
    private float frameBorderSize = 1.0f;         // Thin borders for elegance
    private float windowPaddingX = 8.0f;          // More breathing room
    private float windowPaddingY = 8.0f;
    private float itemSpacingX = 6.0f;            // Increased spacing for better readability
    private float itemSpacingY = 6.0f;
    private float scrollbarSize = 8.0f;           // Slightly wider scrollbar
    private float grabMinSize = 12.0f;            // Larger grab area for better usability
    private float grabRounding = 3.0f;            // Rounded grab handles

    // Color definitions - Modern dark theme with teal accents
    private int[] textColor = {230, 230, 235, 255};                   // Slightly blue-tinted white
    private int[] textDisabledColor = {130, 135, 140, 200};
    private int[] windowBgColor = {22, 25, 28, 245};                  // Deep blue-gray
    private int[] childBgColor = {28, 31, 34, 245};
    private int[] popupBgColor = {32, 35, 38, 240};
    private int[] borderColor = {45, 48, 52, 220};
    private int[] borderShadowColor = {0, 0, 0, 100};
    private int[] frameBgColor = {38, 41, 44, 240};
    private int[] frameBgHoveredColor = {45, 48, 52, 240};
    private int[] frameBgActiveColor = {52, 55, 58, 240};
    private int[] titleBgColor = {22, 25, 28, 245};
    private int[] titleBgActiveColor = {28, 31, 34, 255};
    private int[] titleBgCollapsedColor = {18, 21, 24, 245};
    private int[] menuBarBgColor = {22, 25, 28, 245};
    private int[] scrollbarBgColor = {18, 21, 24, 240};
    private int[] scrollbarGrabColor = {45, 48, 52, 240};
    private int[] scrollbarGrabHoveredColor = {52, 55, 58, 240};
    private int[] scrollbarGrabActiveColor = {58, 61, 64, 240};
    private int[] checkMarkColor = {64, 200, 180, 255};              // Teal accent
    private int[] sliderGrabColor = {45, 48, 52, 240};
    private int[] sliderGrabActiveColor = {64, 200, 180, 240};       // Teal accent
    private int[] buttonColor = {45, 48, 52, 240};
    private int[] buttonHoveredColor = {52, 55, 58, 240};
    private int[] buttonActiveColor = {64, 200, 180, 240};           // Teal accent
    private int[] headerColor = {42, 45, 48, 240};
    private int[] headerHoveredColor = {48, 51, 54, 240};
    private int[] headerActiveColor = {64, 180, 160, 240};           // Slightly darker teal
    private int[] separatorColor = {42, 45, 48, 240};
    private int[] separatorHoveredColor = {52, 55, 58, 240};
    private int[] separatorActiveColor = {64, 200, 180, 240};        // Teal accent
    private int[] resizeGripColor = {45, 48, 52, 240};
    private int[] resizeGripHoveredColor = {52, 55, 58, 240};
    private int[] resizeGripActiveColor = {64, 200, 180, 240};       // Teal accent
    private int[] tabColor = {38, 41, 44, 240};
    private int[] tabHoveredColor = {45, 48, 52, 240};
    private int[] tabActiveColor = {64, 180, 160, 240};              // Slightly darker teal
    private int[] tabUnfocusedColor = {32, 35, 38, 245};
    private int[] tabUnfocusedActiveColor = {42, 45, 48, 240};
    private int[] dockingPreviewColor = {64, 200, 180, 240};         // Teal accent
    private int[] dockingEmptyBgColor = {22, 25, 28, 245};
    private int[] plotLinesColor = {180, 185, 190, 255};             // Slightly blue-tinted
    private int[] plotLinesHoveredColor = {64, 200, 180, 255};       // Teal accent
    private int[] plotHistogramColor = {64, 180, 160, 255};          // Slightly darker teal
    private int[] plotHistogramHoveredColor = {64, 200, 180, 255};   // Teal accent
    private int[] tableHeaderBgColor = {32, 35, 38, 245};
    private int[] tableBorderStrongColor = {58, 61, 64, 220};
    private int[] tableBorderLightColor = {42, 45, 48, 240};
    private int[] tableRowBgColor = {22, 25, 28, 245};
    private int[] tableRowBgAltColor = {28, 31, 34, 245};
    private int[] textSelectedBgColor = {64, 180, 160, 240};         // Slightly darker teal
    private int[] dragDropTargetColor = {64, 200, 180, 240};         // Teal accent
    private int[] navHighlightColor = {64, 200, 180, 240};           // Teal accent
    private int[] navWindowingHighlightColor = {180, 185, 190, 128}; // Slightly blue-tinted
    private int[] navWindowingDimBgColor = {32, 35, 38, 128};
    private int[] modalWindowDimBgColor = {18, 21, 24, 128};

    public void applyCustomColors() {
        try {
            colorsPushed = 0;

            ImGui.PushStyleColor(ImGuiCol.Text, RGBIntToFloat(textColor[0]), RGBIntToFloat(textColor[1]), RGBIntToFloat(textColor[2]), RGBIntToFloat(textColor[3]));
            ImGui.PushStyleColor(ImGuiCol.TextDisabled, RGBIntToFloat(textDisabledColor[0]), RGBIntToFloat(textDisabledColor[1]), RGBIntToFloat(textDisabledColor[2]), RGBIntToFloat(textDisabledColor[3]));
            ImGui.PushStyleColor(ImGuiCol.WindowBg, RGBIntToFloat(windowBgColor[0]), RGBIntToFloat(windowBgColor[1]), RGBIntToFloat(windowBgColor[2]), RGBIntToFloat(windowBgColor[3]));
            ImGui.PushStyleColor(ImGuiCol.ChildBg, RGBIntToFloat(childBgColor[0]), RGBIntToFloat(childBgColor[1]), RGBIntToFloat(childBgColor[2]), RGBIntToFloat(childBgColor[3]));
            ImGui.PushStyleColor(ImGuiCol.PopupBg, RGBIntToFloat(popupBgColor[0]), RGBIntToFloat(popupBgColor[1]), RGBIntToFloat(popupBgColor[2]), RGBIntToFloat(popupBgColor[3]));
            ImGui.PushStyleColor(ImGuiCol.Border, RGBIntToFloat(borderColor[0]), RGBIntToFloat(borderColor[1]), RGBIntToFloat(borderColor[2]), RGBIntToFloat(borderColor[3]));
            ImGui.PushStyleColor(ImGuiCol.BorderShadow, RGBIntToFloat(borderShadowColor[0]), RGBIntToFloat(borderShadowColor[1]), RGBIntToFloat(borderShadowColor[2]), RGBIntToFloat(borderShadowColor[3]));
            ImGui.PushStyleColor(ImGuiCol.FrameBg, RGBIntToFloat(frameBgColor[0]), RGBIntToFloat(frameBgColor[1]), RGBIntToFloat(frameBgColor[2]), RGBIntToFloat(frameBgColor[3]));
            ImGui.PushStyleColor(ImGuiCol.FrameBgHovered, RGBIntToFloat(frameBgHoveredColor[0]), RGBIntToFloat(frameBgHoveredColor[1]), RGBIntToFloat(frameBgHoveredColor[2]), RGBIntToFloat(frameBgHoveredColor[3]));
            ImGui.PushStyleColor(ImGuiCol.FrameBgActive, RGBIntToFloat(frameBgActiveColor[0]), RGBIntToFloat(frameBgActiveColor[1]), RGBIntToFloat(frameBgActiveColor[2]), RGBIntToFloat(frameBgActiveColor[3]));
            ImGui.PushStyleColor(ImGuiCol.TitleBg, RGBIntToFloat(titleBgColor[0]), RGBIntToFloat(titleBgColor[1]), RGBIntToFloat(titleBgColor[2]), RGBIntToFloat(titleBgColor[3]));
            ImGui.PushStyleColor(ImGuiCol.TitleBgActive, RGBIntToFloat(titleBgActiveColor[0]), RGBIntToFloat(titleBgActiveColor[1]), RGBIntToFloat(titleBgActiveColor[2]), RGBIntToFloat(titleBgActiveColor[3]));
            ImGui.PushStyleColor(ImGuiCol.TitleBgCollapsed, RGBIntToFloat(titleBgCollapsedColor[0]), RGBIntToFloat(titleBgCollapsedColor[1]), RGBIntToFloat(titleBgCollapsedColor[2]), RGBIntToFloat(titleBgCollapsedColor[3]));
            ImGui.PushStyleColor(ImGuiCol.MenuBarBg, RGBIntToFloat(menuBarBgColor[0]), RGBIntToFloat(menuBarBgColor[1]), RGBIntToFloat(menuBarBgColor[2]), RGBIntToFloat(menuBarBgColor[3]));
            ImGui.PushStyleColor(ImGuiCol.ScrollbarBg, RGBIntToFloat(scrollbarBgColor[0]), RGBIntToFloat(scrollbarBgColor[1]), RGBIntToFloat(scrollbarBgColor[2]), RGBIntToFloat(scrollbarBgColor[3]));
            ImGui.PushStyleColor(ImGuiCol.ScrollbarGrab, RGBIntToFloat(scrollbarGrabColor[0]), RGBIntToFloat(scrollbarGrabColor[1]), RGBIntToFloat(scrollbarGrabColor[2]), RGBIntToFloat(scrollbarGrabColor[3]));
            ImGui.PushStyleColor(ImGuiCol.ScrollbarGrabHovered, RGBIntToFloat(scrollbarGrabHoveredColor[0]), RGBIntToFloat(scrollbarGrabHoveredColor[1]), RGBIntToFloat(scrollbarGrabHoveredColor[2]), RGBIntToFloat(scrollbarGrabHoveredColor[3]));
            ImGui.PushStyleColor(ImGuiCol.ScrollbarGrabActive, RGBIntToFloat(scrollbarGrabActiveColor[0]), RGBIntToFloat(scrollbarGrabActiveColor[1]), RGBIntToFloat(scrollbarGrabActiveColor[2]), RGBIntToFloat(scrollbarGrabActiveColor[3]));
            ImGui.PushStyleColor(ImGuiCol.CheckMark, RGBIntToFloat(checkMarkColor[0]), RGBIntToFloat(checkMarkColor[1]), RGBIntToFloat(checkMarkColor[2]), RGBIntToFloat(checkMarkColor[3]));
            ImGui.PushStyleColor(ImGuiCol.SliderGrab, RGBIntToFloat(sliderGrabColor[0]), RGBIntToFloat(sliderGrabColor[1]), RGBIntToFloat(sliderGrabColor[2]), RGBIntToFloat(sliderGrabColor[3]));
            ImGui.PushStyleColor(ImGuiCol.SliderGrabActive, RGBIntToFloat(sliderGrabActiveColor[0]), RGBIntToFloat(sliderGrabActiveColor[1]), RGBIntToFloat(sliderGrabActiveColor[2]), RGBIntToFloat(sliderGrabActiveColor[3]));
            ImGui.PushStyleColor(ImGuiCol.Button, RGBIntToFloat(buttonColor[0]), RGBIntToFloat(buttonColor[1]), RGBIntToFloat(buttonColor[2]), RGBIntToFloat(buttonColor[3]));
            ImGui.PushStyleColor(ImGuiCol.ButtonHovered, RGBIntToFloat(buttonHoveredColor[0]), RGBIntToFloat(buttonHoveredColor[1]), RGBIntToFloat(buttonHoveredColor[2]), RGBIntToFloat(buttonHoveredColor[3]));
            ImGui.PushStyleColor(ImGuiCol.ButtonActive, RGBIntToFloat(buttonActiveColor[0]), RGBIntToFloat(buttonActiveColor[1]), RGBIntToFloat(buttonActiveColor[2]), RGBIntToFloat(buttonActiveColor[3]));
            ImGui.PushStyleColor(ImGuiCol.Header, RGBIntToFloat(headerColor[0]), RGBIntToFloat(headerColor[1]), RGBIntToFloat(headerColor[2]), RGBIntToFloat(headerColor[3]));
            ImGui.PushStyleColor(ImGuiCol.HeaderHovered, RGBIntToFloat(headerHoveredColor[0]), RGBIntToFloat(headerHoveredColor[1]), RGBIntToFloat(headerHoveredColor[2]), RGBIntToFloat(headerHoveredColor[3]));
            ImGui.PushStyleColor(ImGuiCol.HeaderActive, RGBIntToFloat(headerActiveColor[0]), RGBIntToFloat(headerActiveColor[1]), RGBIntToFloat(headerActiveColor[2]), RGBIntToFloat(headerActiveColor[3]));
            ImGui.PushStyleColor(ImGuiCol.Separator, RGBIntToFloat(separatorColor[0]), RGBIntToFloat(separatorColor[1]), RGBIntToFloat(separatorColor[2]), RGBIntToFloat(separatorColor[3]));
            ImGui.PushStyleColor(ImGuiCol.SeparatorHovered, RGBIntToFloat(separatorHoveredColor[0]), RGBIntToFloat(separatorHoveredColor[1]), RGBIntToFloat(separatorHoveredColor[2]), RGBIntToFloat(separatorHoveredColor[3]));
            ImGui.PushStyleColor(ImGuiCol.SeparatorActive, RGBIntToFloat(separatorActiveColor[0]), RGBIntToFloat(separatorActiveColor[1]), RGBIntToFloat(separatorActiveColor[2]), RGBIntToFloat(separatorActiveColor[3]));
            ImGui.PushStyleColor(ImGuiCol.ResizeGrip, RGBIntToFloat(resizeGripColor[0]), RGBIntToFloat(resizeGripColor[1]), RGBIntToFloat(resizeGripColor[2]), RGBIntToFloat(resizeGripColor[3]));
            ImGui.PushStyleColor(ImGuiCol.ResizeGripHovered, RGBIntToFloat(resizeGripHoveredColor[0]), RGBIntToFloat(resizeGripHoveredColor[1]), RGBIntToFloat(resizeGripHoveredColor[2]), RGBIntToFloat(resizeGripHoveredColor[3]));
            ImGui.PushStyleColor(ImGuiCol.ResizeGripActive, RGBIntToFloat(resizeGripActiveColor[0]), RGBIntToFloat(resizeGripActiveColor[1]), RGBIntToFloat(resizeGripActiveColor[2]), RGBIntToFloat(resizeGripActiveColor[3]));
            ImGui.PushStyleColor(ImGuiCol.Tab, RGBIntToFloat(tabColor[0]), RGBIntToFloat(tabColor[1]), RGBIntToFloat(tabColor[2]), RGBIntToFloat(tabColor[3]));
            ImGui.PushStyleColor(ImGuiCol.TabHovered, RGBIntToFloat(tabHoveredColor[0]), RGBIntToFloat(tabHoveredColor[1]), RGBIntToFloat(tabHoveredColor[2]), RGBIntToFloat(tabHoveredColor[3]));
            ImGui.PushStyleColor(ImGuiCol.TabActive, RGBIntToFloat(tabActiveColor[0]), RGBIntToFloat(tabActiveColor[1]), RGBIntToFloat(tabActiveColor[2]), RGBIntToFloat(tabActiveColor[3]));
            ImGui.PushStyleColor(ImGuiCol.TabUnfocused, RGBIntToFloat(tabUnfocusedColor[0]), RGBIntToFloat(tabUnfocusedColor[1]), RGBIntToFloat(tabUnfocusedColor[2]), RGBIntToFloat(tabUnfocusedColor[3]));
            ImGui.PushStyleColor(ImGuiCol.TabUnfocusedActive, RGBIntToFloat(tabUnfocusedActiveColor[0]), RGBIntToFloat(tabUnfocusedActiveColor[1]), RGBIntToFloat(tabUnfocusedActiveColor[2]), RGBIntToFloat(tabUnfocusedActiveColor[3]));
            ImGui.PushStyleColor(ImGuiCol.DockingPreview, RGBIntToFloat(dockingPreviewColor[0]), RGBIntToFloat(dockingPreviewColor[1]), RGBIntToFloat(dockingPreviewColor[2]), RGBIntToFloat(dockingPreviewColor[3]));
            ImGui.PushStyleColor(ImGuiCol.DockingEmptyBg, RGBIntToFloat(dockingEmptyBgColor[0]), RGBIntToFloat(dockingEmptyBgColor[1]), RGBIntToFloat(dockingEmptyBgColor[2]), RGBIntToFloat(dockingEmptyBgColor[3]));
            ImGui.PushStyleColor(ImGuiCol.PlotLines, RGBIntToFloat(plotLinesColor[0]), RGBIntToFloat(plotLinesColor[1]), RGBIntToFloat(plotLinesColor[2]), RGBIntToFloat(plotLinesColor[3]));
            ImGui.PushStyleColor(ImGuiCol.PlotLinesHovered, RGBIntToFloat(plotLinesHoveredColor[0]), RGBIntToFloat(plotLinesHoveredColor[1]), RGBIntToFloat(plotLinesHoveredColor[2]), RGBIntToFloat(plotLinesHoveredColor[3]));
            ImGui.PushStyleColor(ImGuiCol.PlotHistogram, RGBIntToFloat(plotHistogramColor[0]), RGBIntToFloat(plotHistogramColor[1]), RGBIntToFloat(plotHistogramColor[2]), RGBIntToFloat(plotHistogramColor[3]));
            ImGui.PushStyleColor(ImGuiCol.PlotHistogramHovered, RGBIntToFloat(plotHistogramHoveredColor[0]), RGBIntToFloat(plotHistogramHoveredColor[1]), RGBIntToFloat(plotHistogramHoveredColor[2]), RGBIntToFloat(plotHistogramHoveredColor[3]));
            ImGui.PushStyleColor(ImGuiCol.TableHeaderBg, RGBIntToFloat(tableHeaderBgColor[0]), RGBIntToFloat(tableHeaderBgColor[1]), RGBIntToFloat(tableHeaderBgColor[2]), RGBIntToFloat(tableHeaderBgColor[3]));
            ImGui.PushStyleColor(ImGuiCol.TableBorderStrong, RGBIntToFloat(tableBorderStrongColor[0]), RGBIntToFloat(tableBorderStrongColor[1]), RGBIntToFloat(tableBorderStrongColor[2]), RGBIntToFloat(tableBorderStrongColor[3]));
            ImGui.PushStyleColor(ImGuiCol.TableBorderLight, RGBIntToFloat(tableBorderLightColor[0]), RGBIntToFloat(tableBorderLightColor[1]), RGBIntToFloat(tableBorderLightColor[2]), RGBIntToFloat(tableBorderLightColor[3]));
            ImGui.PushStyleColor(ImGuiCol.TableRowBg, RGBIntToFloat(tableRowBgColor[0]), RGBIntToFloat(tableRowBgColor[1]), RGBIntToFloat(tableRowBgColor[2]), RGBIntToFloat(tableRowBgColor[3]));
            ImGui.PushStyleColor(ImGuiCol.TableRowBgAlt, RGBIntToFloat(tableRowBgAltColor[0]), RGBIntToFloat(tableRowBgAltColor[1]), RGBIntToFloat(tableRowBgAltColor[2]), RGBIntToFloat(tableRowBgAltColor[3]));
            ImGui.PushStyleColor(ImGuiCol.TextSelectedBg, RGBIntToFloat(textSelectedBgColor[0]), RGBIntToFloat(textSelectedBgColor[1]), RGBIntToFloat(textSelectedBgColor[2]), RGBIntToFloat(textSelectedBgColor[3]));
            ImGui.PushStyleColor(ImGuiCol.DragDropTarget, RGBIntToFloat(dragDropTargetColor[0]), RGBIntToFloat(dragDropTargetColor[1]), RGBIntToFloat(dragDropTargetColor[2]), RGBIntToFloat(dragDropTargetColor[3]));
            ImGui.PushStyleColor(ImGuiCol.NavHighlight, RGBIntToFloat(navHighlightColor[0]), RGBIntToFloat(navHighlightColor[1]), RGBIntToFloat(navHighlightColor[2]), RGBIntToFloat(navHighlightColor[3]));
            ImGui.PushStyleColor(ImGuiCol.NavWindowingHighlight, RGBIntToFloat(navWindowingHighlightColor[0]), RGBIntToFloat(navWindowingHighlightColor[1]), RGBIntToFloat(navWindowingHighlightColor[2]), RGBIntToFloat(navWindowingHighlightColor[3]));
            ImGui.PushStyleColor(ImGuiCol.NavWindowingDimBg, RGBIntToFloat(navWindowingDimBgColor[0]), RGBIntToFloat(navWindowingDimBgColor[1]), RGBIntToFloat(navWindowingDimBgColor[2]), RGBIntToFloat(navWindowingDimBgColor[3]));
            ImGui.PushStyleColor(ImGuiCol.ModalWindowDimBg, RGBIntToFloat(modalWindowDimBgColor[0]), RGBIntToFloat(modalWindowDimBgColor[1]), RGBIntToFloat(modalWindowDimBgColor[2]), RGBIntToFloat(modalWindowDimBgColor[3]));

            colorsPushed = 55;
        } catch (Exception e) {
            System.err.println("Error applying custom colors: " + e.getMessage());
            if (colorsPushed > 0) {
                ImGui.PopStyleColor(colorsPushed);
            }
            colorsPushed = 0;
            throw e;
        }
    }

    public void applyCustomStyles() {
        try {
            stylesPushed = 0;

            ImGui.PushStyleVar(ImGuiStyleVar.WindowRounding, windowRounding);
            ImGui.PushStyleVar(ImGuiStyleVar.FrameRounding, frameRounding);
            ImGui.PushStyleVar(ImGuiStyleVar.FrameBorderSize, frameBorderSize);
            ImGui.PushStyleVar(ImGuiStyleVar.WindowBorderSize, 0.0f);
            ImGui.PushStyleVar(ImGuiStyleVar.PopupBorderSize, 0.0f);
            ImGui.PushStyleVar(ImGuiStyleVar.FramePadding, windowPaddingX, windowPaddingY);
            ImGui.PushStyleVar(ImGuiStyleVar.ItemSpacing, itemSpacingX, itemSpacingY);
            ImGui.PushStyleVar(ImGuiStyleVar.ButtonTextAlign, 0.5f, 0.5f);
            ImGui.PushStyleVar(ImGuiStyleVar.GrabMinSize, grabMinSize);
            ImGui.PushStyleVar(ImGuiStyleVar.GrabRounding, grabRounding);
            ImGui.PushStyleVar(ImGuiStyleVar.ScrollbarSize, scrollbarSize);
            ImGui.PushStyleVar(ImGuiStyleVar.ScrollbarRounding, 0.0f);
            ImGui.PushStyleVar(ImGuiStyleVar.TabRounding, 0.0f);
            ImGui.PushStyleVar(ImGuiStyleVar.CellPadding, 6.0f, 6.0f);
            ImGui.PushStyleVar(ImGuiStyleVar.WindowTitleAlign, 0.5f, 0.5f);
            ImGui.PushStyleVar(ImGuiStyleVar.Alpha, 1.0f);
            ImGui.PushStyleVar(ImGuiStyleVar.DisabledAlpha, 0.6f);
            ImGui.PushStyleVar(ImGuiStyleVar.WindowMinSize, 100.0f, 100.0f);
            ImGui.PushStyleVar(ImGuiStyleVar.SelectableTextAlign, 0.5f, 0.5f);

            stylesPushed = 19;
        } catch (Exception e) {
            System.err.println("Error applying custom styles: " + e.getMessage());
            if (stylesPushed > 0) {
                ImGui.PopStyleVar(stylesPushed);
            }
            stylesPushed = 0;
            throw e;
        }
    }

    public void resetCustomColors() {
        if (colorsPushed > 0) {
            try {
                ImGui.PopStyleColor(colorsPushed);
            } catch (Exception e) {
                System.err.println("Error resetting custom colors: " + e.getMessage());
            } finally {
                colorsPushed = 0;
            }
        }
    }

    public void resetCustomStyles() {
        if (stylesPushed > 0) {
            try {
                ImGui.PopStyleVar(stylesPushed);
            } catch (Exception e) {
                System.err.println("Error resetting custom styles: " + e.getMessage());
            } finally {
                stylesPushed = 0;
            }
        }
    }

    // Helper function to convert RGB int to float (0-255 -> 0.0-1.0)
    private float RGBIntToFloat(int color) {
        return Math.max(0, Math.min(255, color)) / 255.0F;
    }

    // ImGuiCol constants
    public static class ImGuiCol {
        public static final int Text = 0;
        public static final int TextDisabled = 1;
        public static final int WindowBg = 2;
        public static final int ChildBg = 3;
        public static final int PopupBg = 4;
        public static final int Border = 5;
        public static final int BorderShadow = 6;
        public static final int FrameBg = 7;
        public static final int FrameBgHovered = 8;
        public static final int FrameBgActive = 9;
        public static final int TitleBg = 10;
        public static final int TitleBgActive = 11;
        public static final int TitleBgCollapsed = 12;
        public static final int MenuBarBg = 13;
        public static final int ScrollbarBg = 14;
        public static final int ScrollbarGrab = 15;
        public static final int ScrollbarGrabHovered = 16;
        public static final int ScrollbarGrabActive = 17;
        public static final int CheckMark = 18;
        public static final int SliderGrab = 19;
        public static final int SliderGrabActive = 20;
        public static final int Button = 21;
        public static final int ButtonHovered = 22;
        public static final int ButtonActive = 23;
        public static final int Header = 24;
        public static final int HeaderHovered = 25;
        public static final int HeaderActive = 26;
        public static final int Separator = 27;
        public static final int SeparatorHovered = 28;
        public static final int SeparatorActive = 29;
        public static final int ResizeGrip = 30;
        public static final int ResizeGripHovered = 31;
        public static final int ResizeGripActive = 32;
        public static final int Tab = 33;
        public static final int TabHovered = 34;
        public static final int TabActive = 35;
        public static final int TabUnfocused = 36;
        public static final int TabUnfocusedActive = 37;
        public static final int DockingPreview = 38;
        public static final int DockingEmptyBg = 39;
        public static final int PlotLines = 40;
        public static final int PlotLinesHovered = 41;
        public static final int PlotHistogram = 42;
        public static final int PlotHistogramHovered = 43;
        public static final int TableHeaderBg = 44;
        public static final int TableBorderStrong = 45;
        public static final int TableBorderLight = 46;
        public static final int TableRowBg = 47;
        public static final int TableRowBgAlt = 48;
        public static final int TextSelectedBg = 49;
        public static final int DragDropTarget = 50;
        public static final int NavHighlight = 51;
        public static final int NavWindowingHighlight = 52;
        public static final int NavWindowingDimBg = 53;
        public static final int ModalWindowDimBg = 54;
    }

    // ImGuiStyleVar constants
    public static class ImGuiStyleVar {
        public static final int Alpha = 0;
        public static final int DisabledAlpha = 1;
        public static final int WindowPadding = 2;
        public static final int WindowRounding = 3;
        public static final int WindowBorderSize = 4;
        public static final int WindowMinSize = 5;
        public static final int WindowTitleAlign = 6;
        public static final int ChildRounding = 7;
        public static final int ChildBorderSize = 8;
        public static final int PopupRounding = 9;
        public static final int PopupBorderSize = 10;
        public static final int FramePadding = 11;
        public static final int FrameRounding = 12;
        public static final int FrameBorderSize = 13;
        public static final int ItemSpacing = 14;
        public static final int ItemInnerSpacing = 15;
        public static final int IndentSpacing = 16;
        public static final int CellPadding = 17;
        public static final int ScrollbarSize = 18;
        public static final int ScrollbarRounding = 19;
        public static final int GrabMinSize = 20;
        public static final int GrabRounding = 21;
        public static final int TabRounding = 22;
        public static final int ButtonTextAlign = 23;
        public static final int SelectableTextAlign = 24;
        public static final int COUNT = 25;
    }
}