package org.cloudbus.cloudsim.examples;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

public class CloudSimCDEI {

    private static List<Cloudlet> cloudletList;
    private static List<Vm> vmList;
    private static double chaosSeed = Math.random();  // Chaos seed
    private static final int NUM_VMS = 10; // Explicitly define number of VMs
    private static final int NUM_CLOUDLET = 400;
    private static final Random random = new Random(); // Random number generator for VM allocation

    public static void main(String[] args) {
        Log.printLine("Starting CloudSimCDEI...");

        try {
            int num_user = 1; // Number of users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false; // Disable tracing

            // Initialize CloudSim
            CloudSim.init(num_user, calendar, trace_flag);

            // Create Datacenter
            Datacenter datacenter0 = createDatacenter("Datacenter_0");

            // Create Broker
            DatacenterBroker broker = createBroker();
            int brokerId = broker.getId();

            // Simulasi Skenario 1: 400 tugas dan 10 sumber daya
            System.out.println("Running Scenario 1: 400 Cloudlets and 10 VMs");
            runSimulation(broker, brokerId, NUM_CLOUDLET, NUM_VMS);
            Log.printLine("CloudSimCDEI finished!");

        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Unwanted errors happen");
        }
    }

    private static void runSimulation(DatacenterBroker broker, int brokerId, int numCloudlets, int numVMs) {
        // Create Virtual Machines (VM)
        vmList = createVMs(brokerId, numVMs);

        // Submit VM list to Broker
        broker.submitVmList(vmList);

        // Create Cloudlets (Tasks)
        cloudletList = createCloudlets(brokerId, numCloudlets);

        // Submit Cloudlet list to Broker
        broker.submitCloudletList(cloudletList);

        // Start the simulation
        CloudSim.startSimulation();

        // Stop the simulation
        CloudSim.stopSimulation();

        // Get and print results
        List<Cloudlet> newList = broker.getCloudletReceivedList();
        printCloudletList(newList);

        // Apply Chaos DE (CDEI) optimization for VM allocation
        applyChaosDifferentialEvolution(newList, vmList);
    }

    private static Datacenter createDatacenter(String name) {
        List<Host> hostList = new ArrayList<>();
        List<Pe> peList = new ArrayList<>();

        int mips = 10000;  // Increased MIPS to support more VM allocation
        peList.add(new Pe(0, new PeProvisionerSimple(mips))); // Adding a single PE to the host

        int hostId = 0;
        int ram = 8192; // Increased RAM on the host for better performance
        long storage = 1000000; // Host storage in MB
        int bw = 10000; // Host bandwidth

        hostList.add(new Host(
                hostId,
                new RamProvisionerSimple(ram),
                new BwProvisionerSimple(bw),
                storage,
                peList,
                new VmSchedulerTimeShared(peList)  // Use time-shared VM scheduler
        ));

        String arch = "x86"; // Architecture
        String os = "Linux"; // Operating System
        String vmm = "Xen"; // Virtual Machine Monitor
        double timeZone = 10.0;
        double cost = 3.0;
        double costPerMem = 0.05;
        double costPerStorage = 0.001;
        double costPerBw = 0.0;
        LinkedList<Storage> storageList = new LinkedList<>();

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, timeZone, cost, costPerMem, costPerStorage, costPerBw
        );

        Datacenter datacenter = null;
        try {
            datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }


    private static DatacenterBroker createBroker() {
        DatacenterBroker broker = null;
        try {
            broker = new DatacenterBroker("Broker");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return broker;
    }

    private static List<Vm> createVMs(int brokerId, int numVMs) {
        List<Vm> vms = new ArrayList<>();
        for (int vmid = 0; vmid < numVMs; vmid++) {
            int mips = 1000; // Each VM has 1000 MIPS
            long size = 10000; // Image size in MB
            int ram = 512; // VM memory in MB
            long bw = 1000; // Bandwidth
            int pesNumber = 1; // Number of CPUs
            String vmm = "Xen"; // Virtual Machine Monitor

            Vm vm = new Vm(vmid, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
            vms.add(vm);
        }
        return vms;
    }

    private static List<Cloudlet> createCloudlets(int brokerId, int numCloudlets) {
        List<Cloudlet> cloudlets = new ArrayList<>();
        for (int id = 0; id < numCloudlets; id++) {
            // Randomize cloudlet length between 50,000 and 100,000
            long length = 50000 + (long)(random.nextDouble() * (100000 - 50000));
            long fileSize = 300;
            long outputSize = 300;
            UtilizationModel utilizationModel = new UtilizationModelFull();

            Cloudlet cloudlet = new Cloudlet(id, length, 1, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
            cloudlet.setUserId(brokerId);
            
            // Randomly assign to a VM
            int randomVmId = random.nextInt(NUM_VMS);
            cloudlet.setVmId(randomVmId);

            cloudlets.add(cloudlet);
        }
        return cloudlets;
    }

    // Implementing Chaos Differential Evolution (CDEI)
    private static void applyChaosDifferentialEvolution(List<Cloudlet> cloudletList, List<Vm> vmList) {
        int populationSize = 50;
        int numGenerations = 800;
        double crossoverRate = 0.8;
        double mutationFactor = 0.75;

        // Generate initial population using chaos-based initialization
        List<List<Integer>> population = generateChaosInitialPopulation(populationSize, vmList.size(), chaosSeed);

        for (int generation = 0; generation < numGenerations; generation++) {
            List<List<Integer>> newPopulation = new ArrayList<>();
            double bestFitness = Double.MAX_VALUE; // Track the best fitness in each generation

            for (List<Integer> individual : population) {
                // Evaluate the fitness of each individual
                double fitness = evaluateFitness(individual, cloudletList);
                if (fitness < bestFitness) {
                    bestFitness = fitness; // Update the best fitness found
                }

                // Mutation: Create a mutant solution
                List<Integer> mutant = mutate(individual, population, mutationFactor, chaosSeed);

                // Crossover: Apply crossover to create a trial solution
                List<Integer> trial = crossover(individual, mutant, crossoverRate, chaosSeed);

                // Selection: Compare and select the better solution
                if (evaluateFitness(trial, cloudletList) < fitness) {
                    newPopulation.add(trial);
                } else {
                    newPopulation.add(individual);
                }
            }
            population = newPopulation; // Update population for the next generation
        }

        // Find the best solution and print its fitness
        List<Integer> bestSolution = population.get(0);
        double bestFitness = evaluateFitness(bestSolution, cloudletList);

        Log.printLine("Best solution found has fitness value: " + bestFitness);

        applyBestSolution(bestSolution, vmList);
    }



    // Logistic map function for generating chaos values
    private static double logisticMap(double x) {
        return 4.0 * x * (1 - x);  // Logistic map equation
    }

    // Generate initial population based on chaos
    private static List<List<Integer>> generateChaosInitialPopulation(int populationSize, int numVms, double chaosSeed) {
        Random rand = new Random();
        List<List<Integer>> population = new ArrayList<>();
        double chaosValue = chaosSeed;

        for (int i = 0; i < populationSize; i++) {
            List<Integer> individual = new ArrayList<>();
            for (int j = 0; j < numVms; j++) {
                chaosValue = logisticMap(chaosValue);
                individual.add(chaosValue > 0.5 ? 1 : 0); // Allocate based on chaos value
            }
            population.add(individual);
        }
        return population;
    }

    private static List<Integer> mutate(List<Integer> individual, List<List<Integer>> population, double mutationFactor, double chaosSeed) {
        Random rand = new Random();
        List<Integer> mutant = new ArrayList<>(individual);

        // Use chaos for index selection in mutation
        double chaosValue = chaosSeed;
        int idx1 = Math.min(population.size() - 1, Math.max(0, (int) (population.size() * logisticMap(chaosValue))));
        chaosValue = logisticMap(chaosValue);
        int idx2 = Math.min(population.size() - 1, Math.max(0, (int) (population.size() * logisticMap(chaosValue))));
        chaosValue = logisticMap(chaosValue);
        int idx3 = Math.min(population.size() - 1, Math.max(0, (int) (population.size() * logisticMap(chaosValue))));

        for (int i = 0; i < individual.size(); i++) {
            mutant.set(i, population.get(idx1).get(i) + (int) (mutationFactor * (population.get(idx2).get(i) - population.get(idx3).get(i))));
        }
        return mutant;
    }

    private static List<Integer> crossover(List<Integer> parent1, List<Integer> parent2, double crossoverRate, double chaosSeed) {
        Random rand = new Random();
        List<Integer> child = new ArrayList<>(parent1);

        double chaosValue = chaosSeed;
        for (int i = 0; i < parent1.size(); i++) {
            chaosValue = logisticMap(chaosValue);
            if (chaosValue > crossoverRate) {
                child.set(i, parent2.get(i));
            }
        }
        return child;
    }

    private static double evaluateFitness(List<Integer> solution, List<Cloudlet> cloudletList) {
        double totalTime = 0;
        for (int i = 0; i < solution.size(); i++) {
            if (solution.get(i) == 1) {
                totalTime += cloudletList.get(i).getCloudletLength() / 1000.0; 
            }
        }
        return totalTime;
    }

    
    private static void applyBestSolution(List<Integer> bestSolution, List<Vm> vmList) {
        for (int i = 0; i < bestSolution.size(); i++) {
            if (bestSolution.get(i) == 1) {
                Log.printLine("Applying best solution: enabling VM at index " + i);
                vmList.get(i).setCloudletScheduler(new CloudletSchedulerTimeShared());
            }
        }
    }


    private static void printCloudletList(List<Cloudlet> list) {
        int size = list.size();
        Cloudlet cloudlet;
        String indent = "    ";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("Cloudlet ID" + indent + "STATUS" + indent
                + "Data center ID" + indent + "VM ID" + indent + "Time" + indent
                + "Start Time" + indent + "Finish Time");

        DecimalFormat dft = new DecimalFormat("###.##");
        double totalTime = 0;
        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);
            Log.print(indent + cloudlet.getCloudletId() + indent + indent);
            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
                Log.print("SUCCESS");
                Log.printLine(indent + indent + cloudlet.getResourceId()
                        + indent + indent + indent + cloudlet.getVmId()
                        + indent + indent + dft.format(cloudlet.getActualCPUTime())
                        + indent + indent + dft.format(cloudlet.getExecStartTime())
                        + indent + indent + dft.format(cloudlet.getFinishTime()));

                totalTime += cloudlet.getFinishTime();  // Add to total execution time
            }
        }

        // Print average execution time
        Log.printLine("Makespan for all cloudlets: " + totalTime);
        Log.printLine("Average execution time: " + (totalTime / size));
    }
}
