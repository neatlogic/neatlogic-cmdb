[中文](README.md) / English

<p align="left">
    <a href="https://opensource.org/licenses/Apache-2.0" alt="License">
        <img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" /></a>
<a target="_blank" href="https://join.slack.com/t/neatlogichome/shared_invite/zt-1w037axf8-r_i2y4pPQ1Z8FxOkAbb64w">
<img src="https://img.shields.io/badge/Slack-Neatlogic-orange" /></a>
</p>

## about

neatlogic-cmdb is a configuration management module, with neatlogic-autoexec can realize automatic discovery and
collection, with neatlogic-itsm to support modification of configuration items in the process, with neatlogic-pbc to
support automatic push of data to the People's Bank of China.
neatlogic-cmdb adopts the design concept of "everything is CI", and supports the direct conversion of external data into
configuration items for internal consumption of cmdb, such as user information, organizational structure information,
etc.

## Features

### Custom Model

Supports custom models and model inheritance.
![img.png](README_IMAGES/img.png)

- Support multiple attribute types such as reference attribute, password, attachment, text, number, date time,
  expression, etc.
  ![img.png](README_IMAGES/img3.png)
- Relationships support bidirectional rules
  ![img.png](README_IMAGES/img2.png)

### Custom Views

Custom views allow users to have the ability to reorganize data. Users can reorganize the relationship between models
through configuration, or skip intermediate models and go directly to the target model to meet different data
consumption scenarios.
![img.png](README_IMAGES/img4.png)

- Visual data view configuration.
  ![img.png](README_IMAGES/img5.png)
- A small amount of code development for the data view can achieve more personalized display effects.
  ![img.png](README_IMAGES/img7.png)
- Visual topology view configuration.
  ![img.png](README_IMAGES/img6.png)

### Configuration item transaction

Imitate the database to introduce transaction design, configuration item modification supports two-stage submission,
support modification and effective decentralization processing, support transaction recovery, comparison and other
functions

- Global transaction management to facilitate discovery of recently modified configuration items.
  ![img.png](README_IMAGES/img8.png)
- Complete display of configuration item modification records.
  ![img.png](README_IMAGES/img9.png)

### Topology

Support layered, star and other automatic layout methods.

- Model topology
  ![img.png](README_IMAGES/img10.png)
- configuration item topology
  ![img.png](README_IMAGES/img11.png)
- Custom view topology (similar to configuration item topology, but the relationship is generated according to the
  custom view)

### Authorization

In addition to the traditional authorization by model, it also supports the authorization of individual configuration
items through rules.
![img.png](README_IMAGES/img12.png)

- Model permission settings
  ![img.png](README_IMAGES/img13.png)
- Licensing by group
  ![img.png](README_IMAGES/img14.png)

### Global search

- Quickly query configuration items through full-text indexing.
  ![img.png](README_IMAGES/img15.png)

### Others

- Compliance check
- Resource Center (Linkage with Automation)