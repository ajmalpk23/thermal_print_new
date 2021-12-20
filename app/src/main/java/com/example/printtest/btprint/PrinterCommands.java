package com.example.printtest.btprint;

/**
 * Created by https://goo.gl/UAfmBd on 2/6/2017.
 */

public interface PrinterCommands {
    byte HT = 0x9;
    byte LF = 0x0A;
    byte CR = 0x0D;
    byte ESC = 0x1B;
    byte DLE = 0x10;
    byte GS = 0x1D;
    byte FS = 0x1C;
    byte STX = 0x02;
    byte US = 0x1F;
    byte CAN = 0x18;
    byte CLR = 0x0C;
    byte EOT = 0x04;

    byte[] INIT = {27, 64};
    byte[] FEED_LINE = {10};

    byte[] SELECT_FONT_A = {20, 33, 0};

    byte[] SET_BAR_CODE_HEIGHT = {29, 104, 100};
    byte[] PRINT_BAR_CODE_1 = {29, 107, 2};
    byte[] SEND_NULL_BYTE = {0x00};

    byte[] SELECT_PRINT_SHEET = {0x1B, 0x63, 0x30, 0x02};
    byte[] FEED_PAPER_AND_CUT = {0x1D, 0x56, 66, 0x00};

    byte[] SELECT_CYRILLIC_CHARACTER_CODE_TABLE = {0x1B, 0x74, 0x11};

    byte[] SELECT_BIT_IMAGE_MODE = {0x1B, 0x2A, 33, -128, 0};
    byte[] SET_LINE_SPACING_24 = {0x1B, 0x33, 24};
    byte[] SET_LINE_SPACING_30 = {0x1B, 0x33, 30};

    byte[] TRANSMIT_DLE_PRINTER_STATUS = {0x10, 0x04, 0x01};
    byte[] TRANSMIT_DLE_OFFLINE_PRINTER_STATUS = {0x10, 0x04, 0x02};
    byte[] TRANSMIT_DLE_ERROR_STATUS = {0x10, 0x04, 0x03};
    byte[] TRANSMIT_DLE_ROLL_PAPER_SENSOR_STATUS = {0x10, 0x04, 0x04};

    byte[] ESC_FONT_COLOR_DEFAULT = new byte[] { 0x1B, 'r',0x00 };
    byte[] FS_FONT_ALIGN = new byte[] { 0x1C, 0x21, 1, 0x1B,
            0x21, 1 };
    byte[] ESC_ALIGN_LEFT = new byte[] { 0x1b, 'a', 0x00 };
    byte[] ESC_ALIGN_RIGHT = new byte[] { 0x1b, 'a', 0x02 };
    byte[] ESC_ALIGN_CENTER = new byte[] { 0x1b, 'a', 0x01 };
    byte[] ESC_CANCEL_BOLD = new byte[] { 0x1B, 0x45, 0 };


    /*********************************************/
    byte[] ESC_HORIZONTAL_CENTERS = new byte[] { 0x1B, 0x44, 20, 28, 00};
    byte[] ESC_CANCLE_HORIZONTAL_CENTERS = new byte[] { 0x1B, 0x44, 00 };
    /*********************************************/

    byte[] ESC_ENTER = new byte[] { 0x1B, 0x4A, 0x40 };
    byte[] PRINTE_TEST = new byte[] { 0x1D, 0x28, 0x41 };


  

}