package com.github.zxh.classpy.classfile.datatype;

import com.github.zxh.classpy.classfile.ClassComponent;
import com.github.zxh.classpy.classfile.ClassReader;
import com.github.zxh.classpy.common.IntValue;

/**
 * 
 * @author zxh
 */
public abstract class UInt extends ClassComponent implements IntValue {

    private int value;
    
    @Override
    public final int getValue() {
        return value;
    }
    
    @Override
    protected final void readContent(ClassReader reader) {
        value = readUInt(reader);
        describe(value, reader);
    }
    
    protected void describe(int value, ClassReader reader) {
        setDesc(value);
    }
    
    protected abstract int readUInt(ClassReader reader);
    
}
