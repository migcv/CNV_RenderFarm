
//
// StatisticsBranch.java
//
// This program measures and instruments to obtain different statistics
// about Java programs.
//
// Copyright (c) 1998 by Han B. Lee (hanlee@cs.colorado.edu).
// ALL RIGHTS RESERVED.
//
// Permission to use, copy, modify, and distribute this software and its
// documentation for non-commercial purposes is hereby granted provided 
// that this copyright notice appears in all copies.
// 
// This software is provided "as is".  The licensor makes no warrenties, either
// expressed or implied, about its correctness or performance.  The licensor
// shall not be liable for any damages suffered as a result of using
// and modifying this software.
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class StatisticsBranch {
	String class_name_;
	String method_name_;
	int pc_;
	int taken_;
	int not_taken_;

	public StatisticsBranch(String class_name, String method_name, int pc) {
		class_name_ = class_name;
		method_name_ = method_name;
		pc_ = pc;
		taken_ = 0;
		not_taken_ = 0;
	}

	public void print() {
		try {
			File file = new File(
					"/home/ec2-user/CNV_RenderFarm/log/StatisticsBranch" + Thread.currentThread().getId() + ".txt");
			FileWriter logFile = new FileWriter(file, true);
			logFile.write(class_name_ + '\t' + method_name_ + '\t' + pc_ + '\t' + taken_ + '\t' + not_taken_ + "\n");
			logFile.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println(class_name_ + '\t' + method_name_ + '\t' + pc_ + '\t' + taken_ + '\t' + not_taken_);
	}

	public void incrTaken() {
		taken_++;
	}

	public void incrNotTaken() {
		not_taken_++;
	}
}
