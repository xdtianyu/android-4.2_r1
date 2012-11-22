LOCAL_PATH:= $(call my-dir)

codegen_SRC_FILES := \
  AggressiveAntiDepBreaker.cpp \
  AllocationOrder.cpp \
  Analysis.cpp \
  BranchFolding.cpp \
  CalcSpillWeights.cpp \
  CallingConvLower.cpp \
  CodeGen.cpp \
  CodePlacementOpt.cpp \
  CriticalAntiDepBreaker.cpp \
  DFAPacketizer.cpp \
  DeadMachineInstructionElim.cpp \
  DwarfEHPrepare.cpp \
  EarlyIfConversion.cpp \
  EdgeBundles.cpp \
  ExecutionDepsFix.cpp \
  ExpandISelPseudos.cpp \
  ExpandPostRAPseudos.cpp \
  GCMetadata.cpp \
  GCMetadataPrinter.cpp \
  GCStrategy.cpp \
  IfConversion.cpp \
  InlineSpiller.cpp \
  InterferenceCache.cpp \
  IntrinsicLowering.cpp \
  LatencyPriorityQueue.cpp \
  LexicalScopes.cpp \
  LiveDebugVariables.cpp \
  LiveInterval.cpp \
  LiveIntervalAnalysis.cpp \
  LiveIntervalUnion.cpp \
  LiveRangeCalc.cpp \
  LiveRangeEdit.cpp \
  LiveRegMatrix.cpp \
  LiveStackAnalysis.cpp \
  LiveVariables.cpp \
  LLVMTargetMachine.cpp \
  LocalStackSlotAllocation.cpp \
  MachineBasicBlock.cpp \
  MachineBlockFrequencyInfo.cpp \
  MachineBlockPlacement.cpp \
  MachineBranchProbabilityInfo.cpp \
  MachineCSE.cpp \
  MachineCopyPropagation.cpp \
  MachineDominators.cpp \
  MachineFunction.cpp \
  MachineFunctionAnalysis.cpp \
  MachineFunctionPass.cpp \
  MachineFunctionPrinterPass.cpp \
  MachineInstr.cpp \
  MachineInstrBundle.cpp \
  MachineLICM.cpp \
  MachineLoopInfo.cpp \
  MachineLoopRanges.cpp \
  MachineModuleInfo.cpp \
  MachineModuleInfoImpls.cpp \
  MachinePassRegistry.cpp \
  MachineRegisterInfo.cpp \
  MachineSSAUpdater.cpp \
  MachineScheduler.cpp \
  MachineSink.cpp \
  MachineTraceMetrics.cpp \
  MachineVerifier.cpp \
  OcamlGC.cpp \
  OptimizePHIs.cpp \
  PHIElimination.cpp \
  PHIEliminationUtils.cpp \
  Passes.cpp \
  PeepholeOptimizer.cpp \
  PostRASchedulerList.cpp \
  ProcessImplicitDefs.cpp \
  PrologEpilogInserter.cpp \
  PseudoSourceValue.cpp \
  RegAllocBase.cpp \
  RegAllocBasic.cpp \
  RegAllocFast.cpp \
  RegAllocGreedy.cpp \
  RegAllocPBQP.cpp \
  RegisterClassInfo.cpp \
  RegisterCoalescer.cpp \
  RegisterPressure.cpp \
  RegisterScavenging.cpp \
  ScheduleDAG.cpp \
  ScheduleDAGInstrs.cpp \
  ScheduleDAGPrinter.cpp \
  ScoreboardHazardRecognizer.cpp \
  ShadowStackGC.cpp \
  ShrinkWrapping.cpp \
  SjLjEHPrepare.cpp \
  SlotIndexes.cpp \
  SpillPlacement.cpp \
  Spiller.cpp \
  SplitKit.cpp \
  StackColoring.cpp \
  StackProtector.cpp \
  StackSlotColoring.cpp \
  StrongPHIElimination.cpp \
  TailDuplication.cpp \
  TargetFrameLoweringImpl.cpp \
  TargetInstrInfoImpl.cpp \
  TargetLoweringObjectFileImpl.cpp \
  TargetOptionsImpl.cpp \
  TwoAddressInstructionPass.cpp \
  UnreachableBlockElim.cpp \
  VirtRegMap.cpp

# For the host
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(codegen_SRC_FILES)
LOCAL_MODULE:= libLLVMCodeGen

LOCAL_MODULE_TAGS := optional

include $(LLVM_HOST_BUILD_MK)
include $(LLVM_GEN_INTRINSICS_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# For the device
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(codegen_SRC_FILES)
LOCAL_MODULE:= libLLVMCodeGen

LOCAL_MODULE_TAGS := optional

include $(LLVM_DEVICE_BUILD_MK)
include $(LLVM_GEN_INTRINSICS_MK)
include $(BUILD_STATIC_LIBRARY)
