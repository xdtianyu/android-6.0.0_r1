<!--
    Copyright 2015 The Android Open Source Project

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
-->
<?cs # Table of contents for devices.?>
<ul id="nav">
  <li class="nav-section">  <!-- Begin nav section, Device Interfaces -->
    <div class="nav-section-header">
      <a href="<?cs var:toroot ?>devices/index.html">
        <span class="en">Interfaces</span>
      </a>
    </div>
    <ul>
      <li class="nav-section">
      <div class="nav-section-header">
        <a href="<?cs var:toroot ?>devices/audio/index.html">
          <span class="en">Audio</span>
        </a>
      </div>
        <ul>
          <li><a href="<?cs var:toroot ?>devices/audio/terminology.html">Terminology</a></li>
          <li><a href="<?cs var:toroot ?>devices/audio/implement.html">Implementation</a></li>
          <li><a href="<?cs var:toroot ?>devices/audio/attributes.html">Attributes</a></li>
          <li><a href="<?cs var:toroot ?>devices/audio/warmup.html">Warmup</a></li>
          <li class="nav-section">
            <div class="nav-section-header">
              <a href="<?cs var:toroot ?>devices/audio/latency.html">
                <span class="en">Latency</span>
              </a>
            </div>
            <ul>
              <li><a href="<?cs var:toroot ?>devices/audio/latency_contrib.html">Contributors</a></li>
              <li><a href="<?cs var:toroot ?>devices/audio/latency_design.html">Design</a></li>
              <li><a href="<?cs var:toroot ?>devices/audio/latency_measure.html">Measure</a></li>
              <li><a href="<?cs var:toroot ?>devices/audio/testing_circuit.html">Light Testing Circuit</a></li>
              <li><a href="<?cs var:toroot ?>devices/audio/loopback.html">Audio Loopback Dongle</a></li>
              <li><a href="<?cs var:toroot ?>devices/audio/latency_measurements.html">Measurements</a></li>
            </ul>
          </li>
          <li><a href="<?cs var:toroot ?>devices/audio/avoiding_pi.html">Priority Inversion</a></li>
          <li><a href="<?cs var:toroot ?>devices/audio/src.html">Sample Rate Conversion</a></li>
          <li><a href="<?cs var:toroot ?>devices/audio/debugging.html">Debugging</a></li>
          <li><a href="<?cs var:toroot ?>devices/audio/usb.html">USB Digital Audio</a></li>
          <li><a href="<?cs var:toroot ?>devices/audio/tv.html">TV Audio</a></li>
        </ul>
      </li>
      <li><a href="<?cs var:toroot ?>devices/bluetooth.html">Bluetooth</a></li>
      <li class="nav-section">
        <div class="nav-section-header">
          <a href="<?cs var:toroot ?>devices/camera/index.html">
            <span class="en">Camera</span>
          </a>
        </div>
        <ul>
          <li><a href="<?cs var:toroot ?>devices/camera/camera3.html">Camera HAL3</a></li>
          <li><a href="<?cs var:toroot ?>devices/camera/camera3_requests_hal.html">HAL Subsystem</a></li>
          <li><a href="<?cs var:toroot ?>devices/camera/camera3_metadata.html">Metadata and Controls</a></li>
          <li><a href="<?cs var:toroot ?>devices/camera/camera3_3Amodes.html">3A Modes and State</a></li>
          <li><a href="<?cs var:toroot ?>devices/camera/camera3_crop_reprocess.html">Output and Cropping</a></li>
          <li><a href="<?cs var:toroot ?>devices/camera/camera3_error_stream.html">Errors and Streams</a></li>
          <li><a href="<?cs var:toroot ?>devices/camera/camera3_requests_methods.html">Request Creation</a></li>
          <li><a href="<?cs var:toroot ?>devices/camera/versioning.html">Version Support</a></li>
        </ul>
      </li>

      <li><a href="<?cs var:toroot ?>devices/drm.html">DRM</a></li>
      <li class="nav-section">
        <div class="nav-section-header">
          <a href="<?cs var:toroot ?>devices/storage/index.html">
            <span class="en">External Storage</span>
          </a>
        </div>
        <ul>
          <li><a href="<?cs var:toroot ?>devices/storage/config.html">Device Specific Configuration</a></li>
          <li><a href="<?cs var:toroot ?>devices/storage/config-example.html">Typical Configuration Examples</a></li>
        </ul>
      </li>
      <li class="nav-section">
        <div class="nav-section-header">
          <a href="<?cs var:toroot ?>devices/graphics/index.html">
            <span class="en">Graphics</span>
          </a>
        </div>
        <ul>
          <li><a href="<?cs var:toroot ?>devices/graphics/architecture.html">Architecture</a></li>
          <li><a href="<?cs var:toroot ?>devices/graphics/implement.html">Implementation</a></li>
         <li class="nav-section">
            <div class="nav-section-header">
              <a href="<?cs var:toroot ?>devices/graphics/testing.html">
                <span class="en">OpenGL ES Testing</span>
              </a>
            </div>
            <ul>
              <li><a href="<?cs var:toroot ?>devices/graphics/build-tests.html">Building test programs</a></li>
              <li><a href="<?cs var:toroot ?>devices/graphics/port-tests.html">Porting the test framework</a></li>
              <li><a href="<?cs var:toroot ?>devices/graphics/run-tests.html">Running the tests</a></li>
              <li><a href="<?cs var:toroot ?>devices/graphics/automate-tests.html">Automating the tests</a></li>
              <li><a href="<?cs var:toroot ?>devices/graphics/test-groups.html">Using special test groups</a></li>
              <li><a href="<?cs var:toroot ?>devices/graphics/cts-integration.html">Integrating with Android CTS</a></li>
            </ul>
         </li>
        </ul> </li>
      <li class="nav-section">
        <div class="nav-section-header">
          <a href="<?cs var:toroot ?>devices/input/index.html">
            <span class="en">Input</span>
          </a>
        </div>
        <ul>
          <li><a href="<?cs var:toroot ?>devices/input/overview.html">Overview</a></li>
          <li><a href="<?cs var:toroot ?>devices/input/key-layout-files.html">Key Layout Files</a></li>
          <li><a href="<?cs var:toroot ?>devices/input/key-character-map-files.html">Key Character Map Files</a></li>
          <li><a href="<?cs var:toroot ?>devices/input/input-device-configuration-files.html">Input Device Configuration Files</a></li>
          <li><a href="<?cs var:toroot ?>devices/input/migration-guide.html">Migration Guide</a></li>
          <li><a href="<?cs var:toroot ?>devices/input/keyboard-devices.html">Keyboard Devices</a></li>
          <li><a href="<?cs var:toroot ?>devices/input/touch-devices.html">Touch Devices</a></li>
          <li><a href="<?cs var:toroot ?>devices/input/diagnostics.html">Diagnostics</a></li>
          <li><a href="<?cs var:toroot ?>devices/input/getevent.html">Getevent</a></li>
          <li><a href="<?cs var:toroot ?>devices/input/validate-keymaps.html">Validate Keymaps</a></li>
        </ul>
      </li>
      <li><a href="<?cs var:toroot ?>devices/media.html">Media</a></li>
      <li class="nav-section">
        <div class="nav-section-header">
          <a href="<?cs var:toroot ?>devices/sensors/index.html">
            <span class="en">Sensors</span>
          </a>
        </div>
        <ul>
          <li><a href="<?cs var:toroot ?>devices/sensors/sensor-stack.html">Sensor stack</a></li>
          <li><a href="<?cs var:toroot ?>devices/sensors/report-modes.html">Reporting modes</a></li>
          <li><a href="<?cs var:toroot ?>devices/sensors/suspend-mode.html">Suspend mode</a></li>
          <li><a href="<?cs var:toroot ?>devices/sensors/power-use.html">Power consumption</a></li>
          <li><a href="<?cs var:toroot ?>devices/sensors/interaction.html">Interaction</span></a></li>
          <li><a href="<?cs var:toroot ?>devices/sensors/hal-interface.html">HAL interface</a></li>
          <li><a href="<?cs var:toroot ?>devices/sensors/batching.html">Batching</a></li>
          <li><a href="<?cs var:toroot ?>devices/sensors/sensor-types.html">Sensor types</a></li>
          <li><a href="<?cs var:toroot ?>devices/sensors/versioning.html">Version deprecation</a></li>
        </ul>
      </li>
      <li class="nav-section">
        <div class="nav-section-header">
          <a href="<?cs var:toroot ?>devices/tv/index.html">
            <span class="en">TV</span>
          </a>
        </div>
        <ul>
          <li><a href="<?cs var:toroot ?>devices/tv/HDMI-CEC.html">HDMI-CEC control service</a></li>
        </ul>
      </li>
    </ul>
  </li> <!-- End nav-section, Device Interfaces-->


  <li class="nav-section"> <!--Begin nav-section, Core Technologies-->
    <div class="nav-section-header">
      <a href="<?cs var:toroot ?>devices/tech/index.html">
        <span class="en">Core Technologies</span>
      </a>
    </div>

    <ul>
      <li class="nav-section">
        <div class="nav-section-header">
          <a href="<?cs var:toroot ?>devices/tech/dalvik/index.html">
          <span class="en">ART and Dalvik</span></a>
        </div>
        <ul>
          <li><a href="<?cs var:toroot ?>devices/tech/dalvik/dalvik-bytecode.html">Bytecode Format</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/dalvik/dex-format.html">.Dex Format</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/dalvik/instruction-formats.html">Instruction Formats</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/dalvik/constraints.html">Constraints</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/dalvik/configure.html">Configuration</a></li>
        </ul>
      </li>

      <li class="nav-section">
        <div class="nav-section-header">
          <a href="<?cs var:toroot ?>devices/tech/datausage/index.html">
            <span class="en">Data Usage</span>
          </a>
        </div>
        <ul>
          <li><a href="<?cs var:toroot ?>devices/tech/datausage/iface-overview.html">Network interface statistics overview</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/datausage/excluding-network-types.html">Excluding Network Types from Data Usage</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/datausage/tethering-data.html">Tethering Data</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/datausage/usage-cycle-resets-dates.html">Usage Cycle Reset Dates</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/datausage/kernel-overview.html">Kernel Overview</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/datausage/tags-explained.html">Data Usage Tags Explained</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/datausage/kernel-changes.html">Kernel Changes</a></li>
        </ul>
      </li>
      <li class="nav-section">
        <div class="nav-section-header">
          <a href="<?cs var:toroot ?>devices/tech/debug/index.html">
            <span class="en">Debugging and Tuning</span>
          </a>
        </div>
        <ul>
          <li><a href="<?cs var:toroot ?>devices/tech/debug/tuning.html">Performance Tuning</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/debug/native-memory.html">Native Memory Usage</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/debug/dumpsys.html">Dumpsys</a></li>
        </ul>
      </li>

      <li class="nav-section">
        <div class="nav-section-header empty">
          <a href="<?cs var:toroot ?>devices/halref/index.html">
            <span class="en">HAL File Reference</span>
          </a>
        </div>
      </li>

      <li class="nav-section">
        <div class="nav-section-header">
            <a href="<?cs var:toroot ?>devices/tech/ota/index.html">
              <span class="en">OTA Updates</span>
            </a>
        </div>
        <ul>
          <li><a href="<?cs var:toroot ?>devices/tech/ota/tools.html">OTA Tools</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/ota/block.html">Block-based OTA</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/ota/inside_packages.html">Inside OTA Packages</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/ota/device_code.html">Device-Specific Code</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/ota/sign_builds.html">Signing Builds for Release</a></li>
        </ul>
      </li>
      <li class="nav-section">
        <div class="nav-section-header">
          <a href="<?cs var:toroot ?>devices/tech/power/index.html"><span class="en">Power</span></a>
        </div>
        <ul>
          <li><a href="<?cs var:toroot ?>devices/tech/power/batterystats.html">Battery Usage Data</a></li>
        </ul>
      </li>

      <li class="nav-section">
        <div class="nav-section-header">
            <a href="<?cs var:toroot ?>devices/tech/security/index.html">
              <span class="en">Security</span>
            </a>
        </div>
        <ul>
          <li class="nav-section">
            <div class="nav-section-header">
              <a href="<?cs var:toroot ?>devices/tech/security/overview/index.html">
                <span class="en">Overview</span>
              </a>
            </div>
            <ul>
              <li><a href="<?cs var:toroot ?>devices/tech/security/overview/kernel-security.html">Kernel security</a></li>
              <li><a href="<?cs var:toroot ?>devices/tech/security/overview/app-security.html">App security</a></li>
              <li><a href="<?cs var:toroot ?>devices/tech/security/overview/updates-resources.html">Updates and resources</a></li>
              <li class="nav-section">
                <div class="nav-section-header">
                  <a href="<?cs var:toroot ?>devices/tech/security/enhancements/index.html">
                    <span class="en">Enhancements</span>
                  </a>
                </div>
                <ul>
                  <li><a href="<?cs var:toroot ?>devices/tech/security/enhancements/enhancements50.html">Android 5.0</a></li>
                  <li><a href="<?cs var:toroot ?>devices/tech/security/enhancements/enhancements44.html">Android 4.4</a></li>
                  <li><a href="<?cs var:toroot ?>devices/tech/security/enhancements/enhancements43.html">Android 4.3</a></li>
                  <li><a href="<?cs var:toroot ?>devices/tech/security/enhancements/enhancements42.html">Android 4.2</a></li>
                  <li><a href="<?cs var:toroot ?>devices/tech/security/enhancements/enhancements41.html">Android 4.1</a></li>
                </ul>
              </li>
              <li><a href="<?cs var:toroot ?>devices/tech/security/overview/acknowledgements.html">Acknowledgements</a></li>
            </ul>
          </li>
          <li class="nav-section">
            <div class="nav-section-header">
              <a href="<?cs var:toroot ?>devices/tech/security/implement.html">
                <span class="en">Implementation</span>
              </a>
            </div>
            <ul>
              <li><a href="<?cs var:toroot ?>devices/tech/security/encryption/index.html">Encryption</a></li>
              <li class="nav-section">
                <div class="nav-section-header">
                  <a href="<?cs var:toroot ?>devices/tech/security/verifiedboot/index.html">
                    <span class="en">Verified Boot</span>
                  </a>
                </div>
                <ul>
                  <li><a href="<?cs var:toroot ?>devices/tech/security/verifiedboot/verified-boot.html">Verifying boot</a></li>
                  <li><a href="<?cs var:toroot ?>devices/tech/security/verifiedboot/dm-verity.html">Implementing dm-verity</a></li>
                </ul>
              </li>
            <li class="nav-section">
              <div class="nav-section-header">
                <a href="<?cs var:toroot ?>devices/tech/security/selinux/index.html">
                  <span class="en">Security-Enhanced Linux</span>
                </a>
              </div>
              <ul>
                <li><a href="<?cs var:toroot ?>devices/tech/security/selinux/concepts.html">Concepts</a></li>
                <li><a href="<?cs var:toroot ?>devices/tech/security/selinux/implement.html">Implementation</a></li>
                <li><a href="<?cs var:toroot ?>devices/tech/security/selinux/customize.html">Customization</a></li>
                <li><a href="<?cs var:toroot ?>devices/tech/security/selinux/validate.html">Validation</a></li>
              </ul>
            </li>
          </ul>
        </li>
      </ul>

      <li class="nav-section">
        <div class="nav-section-header">
            <a href="<?cs var:toroot ?>devices/tech/resources.html">
              <span class="en">System Resources</span>
            </a>
        </div>
        <ul>
          <li><a href="<?cs var:toroot ?>devices/tech/filesystem-config.html">File System Configuration</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/kernel.html">Kernel Configuration</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/netstats.html">Network Usage Data</a></li>
          <li class="nav-section">
            <div class="nav-section-header">
                <a href="<?cs var:toroot ?>devices/tech/ram/index.html">
                  <span class="en">RAM</span>
                </a>
            </div>
            <ul>
              <li><a href="<?cs var:toroot ?>devices/tech/ram/low-ram.html">Low RAM Configuration</a></li>
              <li><a href="<?cs var:toroot ?>devices/tech/ram/procstats.html">RAM Usage Data</a></li>
            </ul>
          </li>
        </ul>

      <li class="nav-section">
        <div class="nav-section-header">
          <a href="<?cs var:toroot ?>devices/tech/test_infra/tradefed/index.html">
            <span class="en">Testing Infrastructure</span>
          </a>
        </div>
        <ul>
          <li><a href="<?cs var:toroot ?>devices/tech/test_infra/tradefed/fundamentals/index.html">Start Here</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/test_infra/tradefed/fundamentals/machine_setup.html">Machine Setup</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/test_infra/tradefed/fundamentals/devices.html">Working with Devices</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/test_infra/tradefed/fundamentals/lifecycle.html">Test Lifecycle</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/test_infra/tradefed/fundamentals/options.html">Option Handling</a></li>
          <li><a href="<?cs var:toroot ?>devices/tech/test_infra/tradefed/full_example.html">An End-to-End Example</a></li>
          <li id="tradefed-tree-list" class="nav-section">
            <div class="nav-section-header">
              <a href="<?cs var:toroot ?>reference/packages.html">
                <span class="en">Package Index</span>
              </a>
            </div>
          </li>
        </ul>
      </li>
    </ul>
  </li> <!-- End nav-section, Core Technologies -->
</ul> 
