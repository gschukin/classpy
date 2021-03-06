package com.github.zxh.classpy.pecoff.header.file.optional;

import com.github.zxh.classpy.common.FileParseException;
import com.github.zxh.classpy.pecoff.PeComponent;
import com.github.zxh.classpy.pecoff.PeReader;
import com.github.zxh.classpy.pecoff.datatype.UInt16Hex;

/**
 *
 * @author zxh
 */
public class OptionalHeader extends PeComponent {

    // optional header magic number
    private static final int PE32 = 0x10b;
    private static final int PE32_PLUS = 0x20b;
    
    private UInt16Hex magic;
    private StandardFields standardFields;
    private WindowsSpecificFields windowsSpecificFields;
    private DataDirectories dataDirectories;
    
    @Override
    protected void readContent(PeReader reader) {
        magic = reader.readUInt16Hex();
        if (magic.getValue() == PE32) {
            magic.setDesc(magic.getDesc() + "(PE32)");
        } else if (magic.getValue() == PE32_PLUS) {
            magic.setDesc(magic.getDesc() + "(PE32+)");
        } else {
            throw new FileParseException("Invalid optional header magic number!");
        }
        
        boolean isPE32Plus = (magic.getValue() == PE32_PLUS);
        standardFields = new StandardFields(isPE32Plus);
        standardFields.read(reader);
        windowsSpecificFields = new WindowsSpecificFields(isPE32Plus);
        windowsSpecificFields.read(reader);
        dataDirectories = new DataDirectories();
        dataDirectories.read(reader);
    }
    
}
