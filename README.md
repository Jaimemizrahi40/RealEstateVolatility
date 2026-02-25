# Volatility Real Estate Market Analysis

This project implements a Java application for market volatility analysis.  
It calculates risk metrics from 10-year market series, stores them in flat files, and maintains cross-market rankings.

## Overview

The application helps evaluate and compare markets using three core metrics:

- **Standard Deviation**
- **Coefficient of Variation (CV)**
- **Beta** (relative to national benchmark series)

### Key Capabilities

- Add a market and auto-calculate metrics  
- View a market’s rank across all categories  
- Update one metric family (Rent Growth, Vacancy, or Cap Rate)  
- Compare two markets side-by-side  
- Remove markets and clean ranking entries  
- Print ranking tables for all tracked markets  

### Tracked Metrics

- **Rent Growth** (10-year series)  
- **Vacancy** (10-year series)  
- **Cap Rate** (10-year series)  

Each metric is analyzed with:

- Standard Deviation  
- CV  
- Beta vs national reference  

## How It Works

1. Read market 10-year series from user input.  
2. Read national benchmark series from `National.txt`.  
3. Compute SD, CV, and Beta for each metric.  
4. Persist market-level results in `Markets.txt`.  
5. Update sorted rankings in `text.txt`.  
6. Support querying, updating, comparing, and removing markets through the menu.  

## Menu Options

| Option | Action          |
|--------|----------------|
| A      | Add Market      |
| B      | View Market     |
| C      | Update Market   |
| D      | Compare Markets |
| E      | Remove Market   |
| F      | Extract Rankings|
| G      | Exit            |

## File Layout

Required project/runtime files:

- `Volatility.java`  
- `Market.java` (required companion class)  
- `National.txt` (national benchmark inputs)  
- `text.txt` (ranking storage)  
- `Markets.txt` (market storage)  
