package fortest;

import xyz.hrhrng.yodo.SPI;
import xyz.hrhrng.yodo.Strategy;

@SPI(value = "fadf" ,strategy = "mystrategy")
@Strategy(directory = "xyz.j", excludedPackages = {"xyz.z"})
public interface Robot {

}
