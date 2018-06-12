
package software.blob.tv.pdf;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gson.Gson;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import software.blob.tv.obj.*;
import software.blob.tv.Constants;
import software.blob.tv.util.FileUtils;
import software.blob.tv.util.Log;

/**
 * Example to show filling form fields.
 * 
 */
public final class ScheduleForm
{
    private static final String TAG = "ScheduleForm";

    private static final String FORM_FILE = "ScheduleForm.pdf";
    private static final int PAGE_WIDTH = 612;
    private static final int MARGIN = 30;
    private static final int INFO_FONT_SIZE = 22;
    private static final int TBL_Y = 120;
    private static final int TBL_WIDTH = PAGE_WIDTH - MARGIN * 2;
    private static final int TBL_PD = 4;
    private static final int TBL_FS = 12;
    private static final int TBL_PDV = 3;
    private static final int TBL_COL1_WIDTH = TBL_PD * 2 + 52;
    private static final int TBL_COL2_WIDTH = TBL_PD * 2 + 150;
    private static final int TBL_COL3_WIDTH = TBL_WIDTH - (TBL_COL1_WIDTH + TBL_COL2_WIDTH);
    private static final int TBL_CSEP1 = MARGIN + TBL_COL1_WIDTH;
    private static final int TBL_CSEP2 = TBL_CSEP1 + TBL_COL2_WIDTH;

    private Playlist _playlist;
    private final Schedule _schedule;
    private final ChannelInfo _chanInfo;
    private final LogoColors _colors;

    public ScheduleForm(ChannelInfo chanInfo, Playlist playlist, Schedule schedule, LogoColors colors) {
        _chanInfo = chanInfo;
        _playlist = playlist;
        _colors = colors;
        _schedule = schedule;
    }

    public ScheduleForm(ChannelInfo chanInfo, LogoColors colors) {
        this(chanInfo, null, Schedule.load(chanInfo.schedule), colors);
        File playlist = new File(Constants.CHANNEL_PLAYLISTS_DIR, chanInfo.playlist);
        try {
            Gson gs = new Gson();
            _playlist = gs.fromJson(FileUtils.loadJSON(playlist), Playlist.class);
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse playlist file", e);
            _playlist = null;
        }
    }

    public File generate() throws IOException {
        if (_chanInfo == null || _playlist == null || _schedule == null)
            return null;

        // Determine page size
        PDFont font = PDType1Font.HELVETICA;
        //float rowWidth = fontDp(font.getStringWidth("test"), TBL_FONT_SIZE);
        float realTextHeight = fontDp(font.getFontDescriptor()
                .getFontBoundingBox().getHeight(), TBL_FS);
        float tableHeight = TBL_FS * 96 + (_schedule.size() * 2 * TBL_PD); // row height * max # of slots
        float pageHeight = tableHeight + TBL_Y + MARGIN;
        float tableX = MARGIN, tableY = pageHeight - TBL_Y;

        // Create document
        PDDocument doc = new PDDocument();
        PDRectangle pageRect = new PDRectangle(0, 0, PAGE_WIDTH, pageHeight);
        PDPage page = new PDPage(pageRect);
        doc.addPage(page);

        PDImageXObject logo = PDImageXObject.createFromFile(Constants.BTV_HOME + "/i/btv_full.png", doc);

        PDPageContentStream stream = new PDPageContentStream(doc, page,
                PDPageContentStream.AppendMode.APPEND, false);

        // Fill page background
        stream.setNonStrokingColor(Constants.BG2);
        stream.addRect(0, 0, PAGE_WIDTH, pageHeight);
        stream.fill();

        // Logo
        stream.drawImage(logo, 26.64f, pageHeight - 75f - MARGIN, 102.96f, 102.96f);

        // Information
        Date now = new Date();
        stream.setFont(font, INFO_FONT_SIZE);
        stream.setNonStrokingColor(Color.WHITE);
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM d, yyyy");
        String[] info = new String[] {"Schedule for Channel " + _chanInfo.number, sdf.format(now)};

        for (int i = 0; i < info.length; i++) {
            float infoWidth = getTextWidth(font, info[i], INFO_FONT_SIZE);
            float infoHeight = fontDp(font.getFontDescriptor()
                    .getFontBoundingBox().getHeight(), INFO_FONT_SIZE);
            drawText(stream, info[i], PAGE_WIDTH - MARGIN - infoWidth,
                    pageHeight - (MARGIN + 40f + (i * infoHeight)));
        }


        // Table bounds
        stream.setStrokingColor(Constants.BG5);
        stream.setNonStrokingColor(Constants.BG4);
        stream.setLineWidth(1f);
        stream.addRect(tableX, MARGIN, PAGE_WIDTH - MARGIN * 2, tableHeight);
        stream.fillAndStroke();

        // Separate time and show
        stream.moveTo(TBL_CSEP1, tableY);
        stream.lineTo(TBL_CSEP1, MARGIN);

        // Separate show and episode
        stream.moveTo(TBL_CSEP2, tableY);
        stream.lineTo(TBL_CSEP2, MARGIN);
        stream.stroke();

        // Schedule
        stream.setNonStrokingColor(Color.WHITE);
        stream.setFont(font, TBL_FS);
        ScheduleSlot startSlot = _schedule.findSlotByTime(360);
        if (startSlot != null) {
            int start = _schedule.indexOf(startSlot);
            float rowY = tableY;
            for (int i = 0; i < _schedule.size(); i++) {
                ScheduleSlot ss = _schedule.get(i + start, true);

                int runTime = _schedule.getRunTime(ss);
                int lineCount = runTime / 15;
                float rowHeight = lineCount * TBL_FS + TBL_PD * 2;
                rowY -= rowHeight;

                // Colors ribbon
                if (_colors != null) {
                    Color[] showColors = _colors.getShowColors(ss.Show);
                    stream.setLineWidth(1.5f);
                    for (int c = 0; c < showColors.length; c++) {
                        float barY = (rowY + c * 1.5f) + 1f;
                        stream.setStrokingColor(showColors[c]);
                        stream.moveTo(MARGIN, barY);
                        stream.lineTo(PAGE_WIDTH - MARGIN, barY);
                        stream.stroke();
                    }
                }

                // Time slot
                drawTableLine(stream, ss.getFormattedTime(), font, rowY,
                        rowHeight, tableX, TBL_COL1_WIDTH, 1);

                // Show name
                drawTableLine(stream, ss.Show, font, rowY, rowHeight,
                        TBL_CSEP1, TBL_COL2_WIDTH, lineCount);

                // Episode name
                drawTableLine(stream, ss.getEpisodeString(), font, rowY, rowHeight,
                        TBL_CSEP2, TBL_COL3_WIDTH, lineCount);

                // Row separator
                /*stream.setLineWidth(1f);
                stream.setStrokingColor(Constants.BG5);
                stream.moveTo(MARGIN, rowY);
                stream.lineTo(PAGE_WIDTH - MARGIN, rowY);
                stream.stroke();*/
            }
        }
        stream.close();

        // Save and close the filled out form
        sdf = new SimpleDateFormat("yyyy-MM-dd");
        File outDir = new File(Constants.DL_HOME, "channel_" + _chanInfo.number);
        if (!outDir.exists() && !outDir.mkdirs())
            Log.e(TAG, "Failed to create output directory: " + outDir);
        File outFile = new File(outDir, "Schedule_CH"
                + _chanInfo.number + "_" + sdf.format(now) + ".pdf");
        doc.save(outFile);
        doc.close();
        Log.d(TAG, "Generated schedule form for channel " + _chanInfo.number);
        return outFile;
    }

    public static void main(String[] args) throws IOException {
        ChannelInfo[] channels = ChannelInfo.parseChannelList(new File(Constants.CHANNEL_INFO));
        LogoColors colors = LogoColors.load(new File(Constants.LOGO_COLORS));
        for (ChannelInfo ci : channels) {
            ScheduleForm form = new ScheduleForm(ci, colors);
            form.generate();
        }
    }

    private static float fontDp(float size, int fontSize) {
        return size * (fontSize / 1000.0f);
    }

    private static float getTextWidth(PDFont font, String txt, int fontSize) throws IOException {
        return fontDp(font.getStringWidth(txt), fontSize);
    }

    private static void drawText(PDPageContentStream stream,
                                 String txt, float x, float y) throws IOException {
        stream.beginText();
        stream.newLineAtOffset(x, y);
        stream.showText(txt);
        stream.endText();
    }

    /**
     * Convenience method for drawing line-wrapped text in a table cell
     * @param stream Content stream
     * @param txt Text to draw
     * @param font Font to draw with
     * @param rowY Bottom bound of row
     * @param rowHeight Height of row
     * @param colX Left bound of column
     * @param colWidth Width of column
     * @param lineCount Number of lines allowed
     * @throws IOException
     */
    private static void drawTableLine(PDPageContentStream stream, String txt,
                               PDFont font, float rowY, float rowHeight, float colX,
                               float colWidth, int lineCount) throws IOException {
        float col2Wrap = colWidth - TBL_PD * 2;
        if (lineCount > 1) {
            // Wrap text
            String[] lines = wrapText(font, txt, TBL_FS, col2Wrap);
            int minLines = Math.min(lines.length, lineCount);
            float baseline = rowY + (rowHeight - minLines * TBL_FS) / 2 + TBL_PDV;
            for (int l = 0; l < minLines; l++) {
                String line = lines[l];
                float lineWidth = getTextWidth(font, line, TBL_FS);
                drawText(stream, line, colX + (colWidth - lineWidth) / 2f,
                        baseline + (minLines - l - 1) * TBL_FS);
            }
        } else {
            // Shrink text
            float lineWidth = getTextWidth(font, txt, TBL_FS);
            int fontSize = TBL_FS;
            if (lineWidth > col2Wrap) {
                fontSize = (int) (col2Wrap * (TBL_FS / lineWidth));
                lineWidth = col2Wrap;
            }
            stream.setFont(font, fontSize);
            drawText(stream, txt, colX + (colWidth - lineWidth) / 2f,
                    rowY + (rowHeight - fontSize) / 2 + TBL_PDV);
            stream.setFont(font, TBL_FS);
        }
    }

    /**
     * Break text into wrapped lines based on font parameters
     * @param font Font to use
     * @param txt Text to wrap
     * @param fontSize Size of font in pt
     * @param maxWidth Max width for wrapping
     * @return Each line of text
     * @throws IOException
     */
    private static String[] wrapText(PDFont font, String txt,
                                     int fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<String>();
        String[] words = txt.split(" ");
        StringBuilder sb = new StringBuilder();
        int i = 0;
        float totalWidth = 0;
        float spaceWidth = getTextWidth(font, " ", fontSize);
        for (String s : words) {
            float width = getTextWidth(font, s, fontSize);
            totalWidth += width;
            if (totalWidth > maxWidth) {
                if (!sb.toString().isEmpty())
                    lines.add(sb.toString());
                sb = new StringBuilder();
                totalWidth = 0;
            }
            sb.append(s);
            if (i < words.length - 1) {
                sb.append(" ");
                totalWidth += spaceWidth;
            }
            i++;
        }
        if (!sb.toString().isEmpty())
            lines.add(sb.toString());
        return lines.toArray(new String[lines.size()]);
    }
}
